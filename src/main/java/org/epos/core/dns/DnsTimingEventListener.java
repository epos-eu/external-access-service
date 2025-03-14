package org.epos.core.dns;

import okhttp3.EventListener;
import okhttp3.Call;
import okhttp3.HttpUrl;

import java.net.InetAddress;
import java.util.List;

public class DnsTimingEventListener extends EventListener {
    private long dnsStartTime;
    private long dnsEndTime;

    @Override
    public void dnsStart(Call call, String domainName) {
        super.dnsStart(call, domainName);
        dnsStartTime = System.currentTimeMillis();
    }

    @Override
    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        super.dnsEnd(call, domainName, inetAddressList);
        dnsEndTime = System.currentTimeMillis();
        long dnsDuration = dnsEndTime - dnsStartTime;
        System.out.println("DNS resolution duration: " + dnsDuration + "Milliseconds");
    }
}