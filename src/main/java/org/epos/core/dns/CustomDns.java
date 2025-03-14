package org.epos.core.dns;

import okhttp3.Dns;
import okhttp3.OkHttpClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

public class CustomDns implements Dns {
    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        // Check if hostname is an IP address
        if (hostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            try {
                // Reverse lookup to get hostname
                InetAddress inetAddress = InetAddress.getByName(hostname);
                return Collections.singletonList(inetAddress);
            } catch (UnknownHostException e) {
                // Return empty list if lookup fails
                return Collections.emptyList();
            }
        }
        // Default DNS lookup
        return Dns.SYSTEM.lookup(hostname);
    }
}

