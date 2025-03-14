package org.epos.core;

import okhttp3.*;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ExternalServicesRequest {
    private final OkHttpClient client;
    private final int MAX_RETRIES = 3;
    private final int CONNECTION_TIMEOUT = 30; // seconds
    private final int READ_TIMEOUT = 30; // seconds
    private final int WRITE_TIMEOUT = 30; // seconds

    // Define alternative DNS servers
    private static final String[] ALTERNATIVE_DNS_SERVERS = {
            "8.8.8.8",    // Google DNS
            "8.8.4.4",    // Google DNS alternative
            "1.1.1.1",    // Cloudflare DNS
            "9.9.9.9"     // Quad9 DNS
    };

    public ExternalServicesRequest() {
        // Configure DNS resolver with fallback mechanisms
        Dns robustDns = new RobustDns();

        // Build the OkHttp client with our custom configurations
        client = new OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .dns(robustDns)
                .addInterceptor(new RetryInterceptor(MAX_RETRIES))
                .build();
    }

    /**
     * Executes a GET request to the provided URL and returns the response body as a string
     * with enhanced error handling
     */
    public String getResponseBodyWithFallback(String url) {
        String result = "";
        List<String> errors = new ArrayList<>();

        // Try main request
        try {
            result = getResponseBody(url);
            return result;
        } catch (IOException e) {
            errors.add("Primary attempt failed: " + e.getMessage());

            // If DNS is the issue, try with alternative host resolution
            if (e instanceof UnknownHostException) {
                try {
                    // Extract hostname from URL
                    String hostname = new java.net.URL(url).getHost();

                    // Try each alternative DNS server
                    for (String dnsServer : ALTERNATIVE_DNS_SERVERS) {
                        try {
                            String resolvedIp = resolveHostnameUsingDns(hostname, dnsServer);
                            if (resolvedIp != null) {
                                // Replace hostname with IP in URL
                                String ipUrl = url.replace(hostname, resolvedIp);
                                result = getResponseBody(ipUrl);
                                return result;
                            }
                        } catch (Exception dnsEx) {
                            errors.add("DNS server " + dnsServer + " resolution failed: " + dnsEx.getMessage());
                        }
                    }
                } catch (Exception urlEx) {
                    errors.add("URL parsing failed: " + urlEx.getMessage());
                }
            }
        }

        // If all attempts failed, return error summary
        throw new RuntimeException("All attempts to access URL failed. Errors: " + String.join("; ", errors));
    }

    /**
     * Attempt to resolve a hostname using a specific DNS server
     * Note: This is a simplified implementation; in production, use a DNS library
     */
    private String resolveHostnameUsingDns(String hostname, String dnsServer) {
        try {
            // This would typically use a DNS library to query the specific server
            // For demonstration, we're using a simpler approach to show the concept
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            if (addresses.length > 0) {
                return addresses[0].getHostAddress();
            }
        } catch (Exception e) {
            // DNS resolution failed
        }
        return null;
    }

    /**
     * Executes a GET request to the provided URL and returns the response body as a string
     */
    public String getResponseBody(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }

            ResponseBody body = response.body();
            return body != null ? body.string() : "";
        }
    }

    /**
     * Gets the headers from a URL
     */
    public Headers getHeaders(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .head() // HEAD request only gets headers
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }

            return response.headers();
        }
    }

    /**
     * Gets the content type from a URL
     */
    public String getContentType(String url) throws IOException {
        Headers headers = getHeaders(url);
        return headers.get("Content-Type");
    }

    /**
     * Retrieves both the content type and HTTP response code in a single request
     * @return A Response object containing both HTTP code and headers
     */
    public Response getResponseMetadata(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .head() // HEAD request to avoid downloading the body
                .build();

        return client.newCall(request).execute();
        // Note: The caller is responsible for closing this response
    }

    public Map<String, Object> getRedirect(String url) throws IOException {
        Response response = getResponseMetadata(url);

        Map<String, Object> responseMap = new HashMap<>();
        String contentType = response.headers().get("Content-Type");
        String httpStatusCode = String.valueOf(response.code());

        responseMap.put("content-type", contentType);
        responseMap.put("httpStatusCode", httpStatusCode);
        responseMap.put("redirect-url", url);
        return responseMap;
    }

    /**
     * Execute a general request with the robust client
     */
    public Response executeRequest(Request request) throws IOException {
        return client.newCall(request).execute();
    }

    /**
     * Custom DNS resolver that implements fallback mechanisms
     */
    private static class RobustDns implements Dns {
        private final Dns systemDns = Dns.SYSTEM;
        private final OkHttpDnsCache dnsCache = new OkHttpDnsCache();

        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            // First try: Check cache
            List<InetAddress> cachedAddresses = dnsCache.get(hostname);
            if (cachedAddresses != null && !cachedAddresses.isEmpty()) {
                return cachedAddresses;
            }

            // Second try: System DNS
            try {
                List<InetAddress> addresses = systemDns.lookup(hostname);
                if (!addresses.isEmpty()) {
                    dnsCache.put(hostname, addresses);
                    return addresses;
                }
            } catch (UnknownHostException e) {
                // System DNS failed, continue to fallbacks
            }

            // Third try: Check if hostname is already an IP address
            try {
                InetAddress ipAddress = InetAddress.getByName(hostname);
                if (ipAddress.getHostAddress().equals(hostname)) {
                    return Collections.singletonList(ipAddress);
                }
            } catch (UnknownHostException e) {
                // Not an IP address, continue to alternative DNS
            }

            // Fourth try: Alternative DNS servers (Google DNS, Cloudflare DNS)
            try {
                List<InetAddress> addresses = lookupUsingAlternativeDns(hostname);
                if (!addresses.isEmpty()) {
                    dnsCache.put(hostname, addresses);
                    return addresses;
                }
            } catch (Exception e) {
                // Alternative DNS failed, continue
            }

            // All attempts failed, throw exception
            throw new UnknownHostException("Unable to resolve host " + hostname);
        }

        /**
         * Uses alternative DNS servers directly via Java's InetAddress
         * Note: This is simplified and doesn't actually query Google/Cloudflare DNS
         * For a real implementation, consider using dnsjava library
         */
        private List<InetAddress> lookupUsingAlternativeDns(String hostname) {
            List<InetAddress> results = new ArrayList<>();

            // Using just InetAddress.getAllByName as a fallback
            // In a real implementation, you would specifically query alternative DNS servers
            try {
                InetAddress[] addresses = InetAddress.getAllByName(hostname);
                results.addAll(Arrays.asList(addresses));
            } catch (UnknownHostException e) {
                // Alternative lookup failed
            }

            return results;
        }
    }

    /**
     * Simple DNS cache implementation
     */
    private static class OkHttpDnsCache {
        // In a production system, use a more sophisticated cache with TTL and eviction
        private final java.util.Map<String, List<InetAddress>> cache =
                Collections.synchronizedMap(new java.util.HashMap<>());

        public List<InetAddress> get(String hostname) {
            return cache.get(hostname);
        }

        public void put(String hostname, List<InetAddress> addresses) {
            cache.put(hostname, addresses);
        }
    }

    /**
     * Interceptor for automatic retries on transient network failures
     */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;

        public RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            IOException lastException = null;

            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    // Add exponential backoff between retries
                    if (attempt > 0) {
                        try {
                            long backoffMillis = (long) Math.pow(2, attempt) * 1000;
                            Thread.sleep(backoffMillis);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Retry interrupted", e);
                        }
                    }

                    return chain.proceed(request);
                } catch (IOException e) {
                    // Only retry on specific exceptions that might be transient
                    if (isRetryableError(e)) {
                        lastException = e;

                        // If we've exhausted retries, throw the last exception
                        if (attempt == maxRetries - 1) {
                            throw lastException;
                        }
                    } else {
                        // Non-retryable exception, just throw
                        throw e;
                    }
                }
            }

            // Should never reach here, but just in case
            throw new IOException("Unknown error after retry attempts");
        }

        private boolean isRetryableError(IOException e) {
            // Define which exceptions are considered retryable
            // Examples include SocketTimeoutException, ConnectException
            return e instanceof java.net.SocketTimeoutException ||
                    e instanceof java.net.ConnectException ||
                    e instanceof java.net.UnknownHostException ||
                    e.getMessage() != null && e.getMessage().contains("connection");
        }
    }
}