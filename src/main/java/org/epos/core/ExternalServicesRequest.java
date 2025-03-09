package org.epos.core;


import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Dns;
import org.epos.core.ssl.CustomSSLSocketFactory;
import org.epos.core.ssl.LenientX509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;



public class ExternalServicesRequest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExternalServicesRequest.class);

	private static ExternalServicesRequest instance = null;

	static OkHttpClient.Builder builder;
	static SSLContext sslContext = null;

	private static final Map<String, String> dnsCache = new ConcurrentHashMap<>();

	public static ExternalServicesRequest getInstance() {
		//System.setProperty("jsse.enableSNIExtension", "false");
		if (instance == null) {
			instance = new ExternalServicesRequest();
            builder = new OkHttpClient.Builder()
                    //.dns(new CustomDns())
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .callTimeout(30, TimeUnit.SECONDS);
            sslContext = getLenientSSLContext();

			try {
				builder.sslSocketFactory(new CustomSSLSocketFactory(), defaultTrustManager());
			} catch (IOException e) {
				LOGGER.error(e.getLocalizedMessage());
				builder.sslSocketFactory(sslContext.getSocketFactory(), defaultTrustManager());
			}
			builder.hostnameVerifier((hostname, session) -> true);
		}
		return instance;
	}

	public String requestPayload(String url) throws IOException {
        LOGGER.info("Requesting payload for URL -> {}", url);
		return executeRequest(url);
	}

	private String executeRequest(String url) throws IOException {
		Request request = new Request.Builder().url(url).build();

		try (Response response = builder.build().newCall(request).execute()) {
			return handleResponse(response);
		} catch (UnknownHostException e) {
            LOGGER.error("DNS resolution failed for: {}. Trying manual resolution...", url);
			return tryWithManualDns(url);
		}
	}

	private String handleResponse(Response response) throws IOException {
		if (!response.isSuccessful()) {
            LOGGER.error("HTTP request failed with code: {}", response.code());
			return null;
		}
		return response.body().string();
	}

	private String tryWithManualDns(String url) {
		try {
			String hostname = extractHostname(url);
			String ip = resolveIp(hostname);
			if (ip == null) {
                LOGGER.error("Could not resolve IP for: {}", hostname);
				return null;
			}

			LOGGER.info("Resolved IP for {}: {}", hostname, ip);
			String newUrl = url.replace(hostname, ip);

			Request request = new Request.Builder()
					.url(newUrl)
					.header("Host", hostname)
					.build();

			try (Response response = builder.build().newCall(request).execute()) {
				return handleResponse(response);
			}
		} catch (Exception e) {
			LOGGER.error("Manual DNS resolution failed.", e);
		}
		return null;
	}

	private String extractHostname(String url) {
		return url.split("/")[2];
	}

	/**
	 * Risolve l'IP di un hostname con caching.
	 */
	private String resolveIp(String hostname) {
		return dnsCache.computeIfAbsent(hostname, host -> {
			try {
				InetAddress address = InetAddress.getByName(host);
				return address.getHostAddress();
			} catch (UnknownHostException e) {
				LOGGER.error("Failed to resolve IP for: {}", host);
				return null;
			}
		});
	}

	public Map<String, List<String>> requestHeaders(String url) throws IOException {
		LOGGER.info("Requesting headers for URL -> "+url);
		Request request = new Request.Builder()
				.url(url)
				.build();

		try (Response response = builder.build().newCall(request).execute()) {
			return response.headers().toMultimap();
		} catch(javax.net.ssl.SSLPeerUnverifiedException e) {
            LOGGER.error("Error on requesting headers for URL: {} cause: {}", url, e.getLocalizedMessage());
			request = new Request.Builder()
					.url(url.replace("https://", "https://www."))
					.build();
			try (Response response = builder.build().newCall(request).execute()) {
				return response.headers().toMultimap();
			}
		}
	}

	public Map<String, List<String>> requestHeadersUsingHttpsURLConnection(String url) throws IOException {
		LOGGER.info("Requesting headers for URL -> " + url);
		URL requestUrl = new URL(url);
		HttpsURLConnection connection = (HttpsURLConnection) requestUrl.openConnection();

		try {
            return connection.getHeaderFields();
		} finally {
			connection.disconnect();
		}
	}


	public String getContentType(String url) throws IOException {
		LOGGER.info("Requesting content type for URL -> "+url);
		Request request = new Request.Builder()
				.url(url)
				.head()
				.build();

		try (Response response = builder.build().newCall(request).execute()) {
			LOGGER.info("Response: "+response);
			return response.body().contentType().toString();

		} catch(javax.net.ssl.SSLPeerUnverifiedException e) {
			LOGGER.error("Error on requesting content types for URL: "+url+" cause: "+e.getLocalizedMessage());
			request = new Request.Builder()
					.url(url.replace("https://", "https://www."))
					.build();
			try (Response response = builder.build().newCall(request).execute()) {
				return response.body().contentType().toString();
			}
		}
	}

	public Map<String, Object> getRedirect(String url) throws IOException {
		LOGGER.info("Requesting redirect for URL -> "+url);
		Request request = new Request.Builder()
				.url(url)
				.build();

		try (Response response = builder.build().newCall(request).execute()) {
			LOGGER.info("Response: "+response);

			Map<String, Object> responseMap = new HashMap<>();
			String contentType = response.body().contentType().toString();
			String httpStatusCode = Integer.toString(response.code());

			responseMap.put("content-type", contentType);
			responseMap.put("httpStatusCode", httpStatusCode);
			responseMap.put("redirect-url", url);
			return responseMap;
		} catch(javax.net.ssl.SSLPeerUnverifiedException e) {
			LOGGER.error("Error on requesting redirect for URL: "+url+" cause: "+e.getLocalizedMessage());
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
			sslContext.init(null, trustManagers, new java.security.SecureRandom());
			//sslContext.getDefaultSSLParameters().setServerNames(new ArrayList<SNIServerName>());

		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new IllegalStateException(String.format(
					"Failed to obtain an SSL Context for TCS Service request. %s",
					e.toString()));
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
