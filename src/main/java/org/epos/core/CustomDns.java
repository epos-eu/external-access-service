package org.epos.core;

import okhttp3.Dns;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class CustomDns implements Dns {
    private final SimpleResolver resolver;

    public CustomDns() throws UnknownHostException {
        resolver = new SimpleResolver("8.8.8.8"); // Google's public DNS
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        try {
            Lookup lookup = new Lookup(hostname, Type.A);
            lookup.setResolver(resolver);
            Record[] records = lookup.run();
            if (records == null) {
                throw new UnknownHostException("No DNS records found for " + hostname);
            }
            List<InetAddress> addresses = new ArrayList<>();
            for (Record record : records) {
                addresses.add(InetAddress.getByName(record.rdataToString()));
            }
            return addresses;
        } catch (Exception e) {
            throw new UnknownHostException("Failed to resolve " + hostname + ": " + e.getMessage());
        }
    }
}
