package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.datastudio.sdk.api.step.processor.*;
import com.experian.datastudio.sdk.api.step.processor.cache.StepCache;
import com.experian.datastudio.sdk.api.step.processor.cache.StepCacheConfiguration;
import com.experian.datastudio.sdk.api.step.processor.cache.StepCacheManager;
import com.experian.datastudio.sdk.api.step.processor.cache.StepCacheScope;
import com.experian.datastudio.sdk.lib.logging.SdkLogManager;
import com.experian.datastudio.sdk.lib.web.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

class IPGeolocationProcessor implements StepProcessorFunction {

    private static final Logger LOGGER = SdkLogManager.getLogger(IPGeolocation.class, Level.DEBUG);
    private static final String INPUT_ID = "input0";
    private static final String COLUMN_CHOOSER_ID = "columnchooser0";
    private static final String SETTING_ID = "setting0";
    private static final String COLUMN_HEADER_COUNTRY_NAME = "Country Name";
    private static final String CACHE_NAME = "IP Geo Cache - ";
    private static final String INVALID_IP_ADDRESS = "Invalid IP Address";
    private static final String INVALID_JSON_OBJECT = "Invalid JSON object";
    private static final String MAX_RETRIES_REACHED = "Max Retries Reached";
    private static final String SEMAPHORE_TIMEOUT = "Semaphore Timeout";
    private static final String API_RETRY_TTL = "X-Ttl";
    private static final String DEFAULT_API_RETRY_DURATION = "60";
    private static final String API_FIELDS_QUERY = "status,message,country";
    private static final int MAX_CONCURRENT_REQUESTS = 5;
    private static final int MAX_TOTAL_RETRIES = 2 * MAX_CONCURRENT_REQUESTS;
    private static final long ADD_DELAY_API_RETRY_DURATION = 5;
    private static final long CACHE_TTL = 60;
    private static final long SEMAPHORE_ACQUIRE_TIMEOUT = 60;
    private static final Pattern PATTERN = Pattern.compile("^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$");

    // Create http client object
    private final WebHttpClient client = createWebHttpClient();
    private final Map<String, String> queryString = new HashMap<>();

    // Throttling (Configurable)
    // No more than MAX_CONCURRENT_REQUESTS request active at any one time
    private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);

    // Track number of retries if HTTP error code is returned
    private int numberOfRetries = 0;

    @Override
    public long execute(StepProcessorContext processorContext, OutputColumnManager outputColumnManager) {
        // Retrieve langStepSetting - field retrieved from the IP Geolocation step setting
        final String langStepSetting = processorContext.getStepSettingFieldValueAsString(SETTING_ID).orElse("");
        queryString.put("lang", langStepSetting);
        // Select fields for queryString to reduce response size
        queryString.put("fields", API_FIELDS_QUERY);

        // Set step cache configuration
        // Name             : IP Geolocation Cache
        // Time to live     : 60 minutes
        // Scope            : Workflow
        // Key-value type   : String - String
        final StepCacheConfiguration<String, String> cacheConfiguration = processorContext
                .getCacheConfigurationBuilder()
                .withCacheName(CACHE_NAME + langStepSetting)
                .withTtlForUpdate(CACHE_TTL, TimeUnit.MINUTES)
                .withScope(StepCacheScope.WORKFLOW)
                .build(String.class, String.class);

        // Get or create cache
        final StepCacheManager cacheManager = processorContext.getCacheManager();
        final StepCache<String, String> cache = cacheManager.getOrCreateCache(cacheConfiguration);
        LOGGER.info("Get or create cache={}", cacheConfiguration.getCacheName());

        // Processor for input column
        final ProcessorInputContext inputContext = processorContext.getInputContext(INPUT_ID).orElseThrow(IllegalStateException::new);
        final long rowCount = inputContext.getRowCount();

        final List<InputColumn> columnChooserValues = processorContext.getColumnFromChooserValues(COLUMN_CHOOSER_ID);
        if (!columnChooserValues.isEmpty()) {
            final InputColumn inputColumn = columnChooserValues.get(0);
            geolocateIp(inputColumn, cache, rowCount);
            outputColumnManager.onValue(COLUMN_HEADER_COUNTRY_NAME, rowIndex -> {
                // Retrieve IP address from input column and return cached value
                final String ipAddress = inputColumn.getStringValueAt(rowIndex);
                return cache.get(ipAddress);
            });
        }
        return rowCount;
    }

    private void geolocateIp(InputColumn inputColumn,
                             StepCache<String, String> cache,
                             long rowCount) {

        // Retrieve all IP addresses from input column
        for (long currentRow = 0; currentRow < rowCount; currentRow++) {
            final String ipAddress = inputColumn.getStringValueAt(currentRow);

            // Get from cache (returns null if cache is empty)
            String result = cache.get(ipAddress);
            LOGGER.debug("Get from cache={}, key={}, value={}", CACHE_NAME, ipAddress, result);

            // Get from JSON endpoint (if cache is empty)
            if (result == null) {
                requestCountryFromEndpoint(ipAddress, cache);
            }
        }

        // Check if any HTTP requests are still pending
        try {
            if (!semaphore.tryAcquire(MAX_CONCURRENT_REQUESTS, SEMAPHORE_ACQUIRE_TIMEOUT, TimeUnit.MINUTES)) {
                // If semaphore has timed out, throw exception
                throw new RetryException(SEMAPHORE_TIMEOUT);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaphore.release(MAX_CONCURRENT_REQUESTS);
        }
    }

    private void requestCountryFromEndpoint(final String ipAddress,
                                            final StepCache<String, String> cache) {

        // HTTP Request is made here using Aperture Data Studio's http module in sdklib
        // CompletableFuture<WebHttpResponse>> demonstrates the ability to handle concurrent asynchronous requests

        // Concurrent asynchronous requests
        if (validIpV4(ipAddress)) {
            // Send http request through WebHttpClient
            callRestService(ipAddress, cache);
        } else {
            storeCountryInCache(cache, ipAddress, INVALID_IP_ADDRESS);
        }
    }

    private void callRestService(final String ip,
                                 final StepCache<String, String> cache) {
        if (numberOfRetries > MAX_TOTAL_RETRIES) {
            throw new RetryException(MAX_RETRIES_REACHED);
        }
        try {
            if (!semaphore.tryAcquire(SEMAPHORE_ACQUIRE_TIMEOUT, TimeUnit.MINUTES)) {
                // If semaphore has timed out, throw exception
                throw new RetryException(SEMAPHORE_TIMEOUT);
            }

            // Create http GET request object
            final WebHttpRequest request = createWebHttpRequest(ip);
            final CompletableFuture<WebHttpResponse> webHttpResponse = client.sendAsync(request);

            webHttpResponse
                    .thenAccept(response -> {
                        // Retry up to MAX_RETRIES if HTTP response status code is not 200
                        HttpStatus webHttpResponseStatus = response.getStatus().getStatus();
                        if (webHttpResponseStatus != HttpStatus.OK) {
                            numberOfRetries++;

                            // Retrieve X-ttl from HTTP Header to determine time to wait for retrying
                            final String xttlValue = response
                                    .getHeaders()
                                    .stream()
                                    .filter(webHttpHeader -> webHttpHeader
                                            .getKey()
                                            .equals(API_RETRY_TTL))
                                    .map(WebHttpHeader::getValue)
                                    .findAny()
                                    .orElse(DEFAULT_API_RETRY_DURATION);
                            long retryDuration = Long.parseLong(xttlValue) + ADD_DELAY_API_RETRY_DURATION;
                            LOGGER.info("Retry request for {} after {} seconds", ip, retryDuration);

                            try {
                                TimeUnit.SECONDS.sleep(retryDuration);
                            } catch (InterruptedException e) {
                                LOGGER.error(e.getLocalizedMessage(), e);
                                Thread.currentThread().interrupt();
                            }
                            semaphore.release();
                            if (numberOfRetries <= MAX_TOTAL_RETRIES) {
                                callRestService(ip, cache);
                            }
                        } else {
                            // Reset number of retries
                            numberOfRetries = 0;
                            semaphore.release();

                            String value = extractCountryFromWebHttpResponses(response);
                            storeCountryInCache(cache, ip, value);
                            LOGGER.info("{}: {}", ip, value);
                        }
                    });
        } catch (InterruptedException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    private String extractCountryFromWebHttpResponses(WebHttpResponse webHttpResponse) {
        try {
            String webHttpResponseBody = webHttpResponse.getBody();
            JSONObject jsonObject = new JSONObject(webHttpResponseBody);
            String countryName = (String) jsonObject.opt("country");
            String errorMessage = StringUtils.capitalize((String) jsonObject.opt("message"));
            return (countryName != null) ? countryName : errorMessage;
        } catch (JSONException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            return INVALID_JSON_OBJECT;
        }
    }

    private void storeCountryInCache(StepCache<String, String> cache, String cacheKey, String result) {
        cache.put(cacheKey, result);
    }

    private WebHttpRequest createWebHttpRequest(String ip) {
        // IP Geolocation - JSON endpoint
        // Source: https://ip-api.com/docs/api:json
        // Max requests per minute: 45/minute
        return WebHttpRequest
                .builder()
                .get("http://ip-api.com/json/" + ip)
                .withQueryString(queryString)
                .build();
    }

    private WebHttpClient createWebHttpClient() {
        return WebHttpClient
                .builder()
                .withHttpVersion(HttpVersion.HTTP1_1)
                .withProxy(Proxy.NO_PROXY)
                .withConnectionTimeout(10L, TimeUnit.SECONDS)
                .withSocketTimeout(10L, TimeUnit.SECONDS)
                .build();
    }

    private boolean validIpV4(final String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return PATTERN.matcher(ip).matches();
    }
}
