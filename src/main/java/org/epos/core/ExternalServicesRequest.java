package org.epos.core;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

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

	//final OkHttpClient client = new OkHttpClient();
	static OkHttpClient.Builder builder;
	static SSLContext sslContext = null;


	public static ExternalServicesRequest getInstance() {
		//System.setProperty("jsse.enableSNIExtension", "false");
		if (instance == null) {
			instance = new ExternalServicesRequest();
			builder = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).connectTimeout(10, TimeUnit.SECONDS);
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

	public Request generateRequest(String urlString){
        return new Request.Builder()
				.url(urlString)
				.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
				.addHeader("Accept", "application/json, text/plain, */*")
				.addHeader("Accept-Language", "en-US,en;q=0.9")
				.addHeader("Connection", "keep-alive")
				.addHeader("Host", getHostFromUrl(urlString)) // Extracts Host dynamically
				.build();
    }

	public Request generateRequestHead(String urlString){
		return new Request.Builder()
				.url(urlString)
				.head()
				.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
				.addHeader("Accept", "application/json, text/plain, */*")
				.addHeader("Accept-Language", "en-US,en;q=0.9")
				.addHeader("Connection", "keep-alive")
				.addHeader("Host", getHostFromUrl(urlString)) // Extracts Host dynamically
				.build();
	}

	private static String getHostFromUrl(String urlString) {
		return urlString.replaceFirst("https?://", "").split("/")[0];
	}

	public String requestPayload(String url) throws IOException {
		LOGGER.info("Requesting payload for URL -> "+url);
		Request request = generateRequest(url);

		try (Response response = builder.build().newCall(request).execute()) {
			return response.body().string();
		} catch(javax.net.ssl.SSLPeerUnverifiedException e) {
			LOGGER.error("Error on requesting payload for URL: "+url+" cause: "+e.getLocalizedMessage());
			request = new Request.Builder()
					.url(url.replace("https://", "https://www."))
					.build();
			try (Response response = builder.build().newCall(request).execute()) {
				return response.body().string();
			}
		}
	}

	public Map<String, List<String>> requestHeaders(String url) throws IOException {
		LOGGER.info("Requesting headers for URL -> "+url);
		Request request = generateRequest(url);

		try (Response response = builder.build().newCall(request).execute()) {
			return response.headers().toMultimap();
		} catch(javax.net.ssl.SSLPeerUnverifiedException e) {
			LOGGER.error("Error on requesting headers for URL: "+url+" cause: "+e.getLocalizedMessage());
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
			Map<String, List<String>> headers = connection.getHeaderFields();
			return headers;
		} finally {
			connection.disconnect();
		}
	}

	public String getContentType(String url) throws IOException {
		LOGGER.info("Requesting content type for URL -> "+url);
		Request request = generateRequestHead(url);

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
		Request request = generateRequest(url);

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
			request = generateRequest(url.replace("https://", "https://www."));
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