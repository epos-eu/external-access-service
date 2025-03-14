package org.epos.core.dns;

import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

public class ReverseApiDns implements Dns {
    private static final String REVERSE_DNS_API = "https://api.hackertarget.com/reversedns/?q=";

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        // Check if the input is an IP address
        if (hostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            String resolvedHostname = getHostnameFromApi(hostname);
            if (resolvedHostname != null) {
                try {
                    // Convert the hostname back to an InetAddress
                    return Collections.singletonList(InetAddress.getByName(resolvedHostname));
                } catch (UnknownHostException e) {
                    return Collections.emptyList();
                }
            }
        }
        // Default DNS lookup for normal hostnames
        return Dns.SYSTEM.lookup(hostname);
    }

    private String getHostnameFromApi(String ip) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(REVERSE_DNS_API + ip)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string().trim();
                // Some APIs return a full response, so we might need to extract the first valid hostname
                if (!body.isEmpty() && !body.contains("error")) {
                    return body.split("\n")[0]; // First line often contains the primary hostname
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to resolve hostname via API: " + e.getMessage());
        }
        return null; // Return null if resolution fails
    }

}
