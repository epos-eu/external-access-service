package org.epos.core;


import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.epos.core.ssl.CustomSSLSocketFactory;
import org.epos.core.ssl.LenientX509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;

public class ExternalServicesRequest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExternalAccessHandler.class);
	
	private static ExternalServicesRequest instance = null;
	
	//final OkHttpClient client = new OkHttpClient();
	static OkHttpClient.Builder builder;
	static SSLContext sslContext = null;
	
	public static ExternalServicesRequest getInstance() {
		System.setProperty("jsse.enableSNIExtension", "false");
        if (instance == null) {
            instance = new ExternalServicesRequest();
            builder = new OkHttpClient.Builder();
            /*ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
                    .cipherSuites(
                            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                            CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA)
                    .build();
            builder.connectionSpecs(Collections.singletonList(spec));*/
            sslContext = getLenientSSLContext();
            try {
				builder.sslSocketFactory(new CustomSSLSocketFactory(), defaultTrustManager());
			} catch (IOException e) {
				LOGGER.error(e.getLocalizedMessage());
				builder.sslSocketFactory(sslContext.getSocketFactory(), defaultTrustManager());
			}
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            builder.callTimeout(30, TimeUnit.SECONDS);
        }
        return instance;
    }
	
	public static void main(String[] args) {
		String url = "https://insar.irea.cnr.it/geoserver/wms?service=WMS&version=1.3.0&REQUEST=GetMap&layers=geonode%3Adtslos_cnrirea_20160610_20220721_wrvc&width=256&height=256&srs=CRS%3A84&format=image%2Fjpeg&BBOX=12.2423802309373,40.535845278100204,13,41.50&transparent=true";
		
		HashMap<String,Object> responseMap = new HashMap<String, Object>();
		try {
			responseMap.put("content", ExternalServicesRequest.getInstance().requestPayloadImage(url));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(responseMap);
	}

	public String requestPayload(String url) throws IOException {
		Request request = new Request.Builder()
				.url(url)
				.build();

		try (Response response = builder.build().newCall(request).execute()) {
			return response.body().string();
		} catch(javax.net.ssl.SSLPeerUnverifiedException e) {
			LOGGER.error(e.getMessage());
			request = new Request.Builder()
					.url(url.replace("https://", "https://www."))
					.build();
			try (Response response = builder.build().newCall(request).execute()) {
				return response.body().string();
			} 
		}
	}
	
	public String requestPayloadImage(String url) throws IOException {
		
		Request request = new Request.Builder()
				.url(url)
				.build();

		try (Response response = builder.build().newCall(request).execute()) {
			//return Base64.encode(response.body().bytes());
			return Base64.getEncoder().encodeToString(response.body().bytes());
		} catch(javax.net.ssl.SSLPeerUnverifiedException e) {
			LOGGER.error(e.getMessage());
			request = new Request.Builder()
					.url(url.replace("https://", "https://www."))
					.build();
			try (Response response = builder.build().newCall(request).execute()) {
				//return Base64.encode(response.body().bytes());
				return Base64.getEncoder().encodeToString(response.body().bytes());
			} 
		}
	}
	
	public Map<String, List<String>> requestHeaders(String url) throws IOException {
		Request request = new Request.Builder()
				.url(url)
				.build();

		try (Response response = builder.build().newCall(request).execute()) {
			return response.headers().toMultimap();
		} catch(javax.net.ssl.SSLPeerUnverifiedException e) {
			LOGGER.error(e.getMessage());
			request = new Request.Builder()
					.url(url.replace("https://", "https://www."))
					.build();
			try (Response response = builder.build().newCall(request).execute()) {
				return response.headers().toMultimap();
			} 
		}
	}
	
	
	public String getHttpStatusCode(String url) throws IOException {
		Request request = new Request.Builder()
				.url(url)
				.build();

		try (Response response = builder.build().newCall(request).execute()) {
			return Integer.toString(response.code());
		} catch(javax.net.ssl.SSLPeerUnverifiedException e) {
			LOGGER.error(e.getMessage());
			request = new Request.Builder()
					.url(url.replace("https://", "https://www."))
					.build();
			try (Response response = builder.build().newCall(request).execute()) {
				return Integer.toString(response.code());
			} 
		}
	}
	
	public Map<String, Object> getRedirect(String url) throws IOException {
		Request request = new Request.Builder()
				.url(url)
				.build();

		try (Response response = builder.build().newCall(request).execute()) {
			System.out.println(response);

			Map<String, Object> responseMap = new HashMap<>();
			String contentType = response.body().contentType().toString();	
			String httpStatusCode = Integer.toString(response.code());
			
			responseMap.put("content-type", contentType);
			responseMap.put("httpStatusCode", httpStatusCode);
			responseMap.put("redirect-url", url);
			return responseMap;
		} catch(javax.net.ssl.SSLPeerUnverifiedException e) {
			LOGGER.error(e.getMessage());
			request = new Request.Builder()
					.url(url.replace("https://", "https://www."))
					.build();
			try (Response response = builder.build().newCall(request).execute()) {
				Map<String, Object> responseMap = new HashMap<>();
				String contentType = response.body().contentType().toString();	
				String httpStatusCode = Integer.toString(response.code());
				
				responseMap.put("content-type", contentType);
				responseMap.put("httpStatusCode", httpStatusCode);
				responseMap.put("redirect-url", url);
				return responseMap;
			} 
		}
	}
	
	/*
	 * SSL Context
	 */
	
	private static SSLContext getLenientSSLContext()
	{
		X509TrustManager[] trustManagers = LenientX509TrustManager.wrap(defaultTrustManager());
		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustManagers, null);
			sslContext.getDefaultSSLParameters().setServerNames(new ArrayList<SNIServerName>());
			
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new IllegalStateException(String.format(
					"Failed to obtain an SSL Context for TCS Service request. %s",
					e.getMessage()));
		}
		return sslContext;
	}
	
	private static X509TrustManager defaultTrustManager() 
	{
	    TrustManager[] trustManagers = defaultTrustManagerFactory().getTrustManagers();
	    if (trustManagers.length != 1) {
	        throw new IllegalStateException("Unexpected default trust managers:"
	                                        + Arrays.toString(trustManagers));
	    }
	    TrustManager trustManager = trustManagers[0];
	    if (trustManager instanceof X509TrustManager) {
	        return (X509TrustManager) trustManager;
	    }
	    throw new IllegalStateException("'" + trustManager + "' is not a X509TrustManager");
	}


	private static TrustManagerFactory defaultTrustManagerFactory() 
	{
	    try {
	        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	        tmf.init((KeyStore) null);
	        return tmf;
	    } catch (NoSuchAlgorithmException | KeyStoreException e) {
	        throw new IllegalStateException("Can't load default trust manager", e);
	    }
	}

}
