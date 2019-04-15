package com.experian.aperture.datastudio.sdk.step.examples.testframework;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;

/**
 * Factory to create http client.
 */
public final class HttpClientFactory {
    private static final Logger LOG = LogManager.getLogger();

    private HttpClientFactory() {
    }

    public static HttpAsyncClient getHttpClient() {
        return LazyHolder.CLIENT;
    }

    private static class LazyHolder {
        private static final HttpAsyncClient CLIENT = createClient();

        private static HttpAsyncClient createClient() {
            try {
                final SSLContext builder = new SSLContextBuilder()
                        .loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true)
                        .build();

                final HttpAsyncClient client = HttpAsyncClients.custom()
                        .setSSLContext(builder)
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build();

                ((CloseableHttpAsyncClient) client).start();
                return client;
            } catch (final Exception e) {
                LOG.error(e);
            }
            return null;
        }
    }
}
