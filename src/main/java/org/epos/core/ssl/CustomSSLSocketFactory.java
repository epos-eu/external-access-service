package org.epos.core.ssl;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class CustomSSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory defaultFactory;
    public CustomSSLSocketFactory() throws IOException {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }};

        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init((KeyManager[])null, trustAllCerts, new SecureRandom());
            defaultFactory = sslContext.getSocketFactory();
        } catch (KeyManagementException | NoSuchAlgorithmException var3) {
            throw new IOException("Can't create unsecure trust manager");
        }
    }
    @Override
    public String[] getDefaultCipherSuites() {
       return defaultFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return defaultFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        //magic happens here, we send null as hostname
        return defaultFactory.createSocket(socket, null, i, b);
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
        return defaultFactory.createSocket(s,i);
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
        return defaultFactory.createSocket(s,i,inetAddress,i1);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return defaultFactory.createSocket(inetAddress, i);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return defaultFactory.createSocket(inetAddress,i, inetAddress1, i1);
    }
}