package com.experian.aperture.datastudio.sdk.step.examples.testframework;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * The color service to retrieve color information based on id.
 */
class ColorService {
    private static final String COLOR_RESOURCE = "api/unknown/%s";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private String baseUri = "https://reqres.in/";
    private HttpAsyncClient asyncClient;

    ColorService(final HttpAsyncClient httpAsyncClient) {
        this.asyncClient = httpAsyncClient;
    }

    public void setBaseUri(final String uri) {
        this.baseUri = uri;
    }

    /**
     * Get color based on the specified {@code resourceId}.
     *
     * @param resourceId the resource id.
     * @return task that will complete in future.
     */
    CompletableFuture<ColorResponse> get(final String resourceId) {
        final String uriFormat = this.baseUri + COLOR_RESOURCE;
        final HttpGet httpGet = new HttpGet(String.format(uriFormat, resourceId));
        final CompletableFuture<ColorResponse> task = new CompletableFuture<>();

        this.asyncClient.execute(httpGet, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse result) {
                try {
                    task.complete(toColorResponse(EntityUtils.toString(result.getEntity())));
                } catch (final IOException e) {
                    task.completeExceptionally(e);
                }
            }

            @Override
            public void failed(final Exception ex) {
                task.completeExceptionally(ex);
            }

            @Override
            public void cancelled() {
                task.cancel(true);
            }
        });
        return task;
    }

    private ColorResponse toColorResponse(final String jsonResult) {
        try {
            return OBJECT_MAPPER.readValue(jsonResult, Response.class).getData();
        } catch (final IOException ignored) {
            return null;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Response {
        @JsonProperty("data")
        private final ColorResponse data;

        @JsonCreator
        Response(@JsonProperty("name") final ColorResponse data) {
            this.data = data;
        }

        public ColorResponse getData() {
            return data;
        }
    }
}
