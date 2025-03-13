package org.epos.core;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
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

	private static final int MAX_RETRIES = 3; // Number of retries
	private static final long RETRY_DELAY_MS = 2000; // 2 seconds retry delay

	private static ExternalServicesRequest instance = null;

	//final OkHttpClient client = new OkHttpClient();
	static OkHttpClient.Builder builder;
	static SSLContext sslContext = null;


	public static ExternalServicesRequest getInstance() {
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
			//builder.dns(ExternalServicesRequest::resolveWithFallback).retryOnConnectionFailure(true);
		}
		return instance;
	}

	private static List<InetAddress> resolveWithFallback(String hostname) throws UnknownHostException {
		// List of DNS providers in order of preference
		List<String> dnsProviders = Arrays.asList(
				"8.8.8.8", "8.8.4.4",         // Google Public DNS
				"1.1.1.1", "1.0.0.1",         // Cloudflare DNS
				"208.67.222.222", "208.67.220.220" // OpenDNS
		);

		// Try each DNS provider in order
		for (String dns : dnsProviders) {
			try {
				return Collections.singletonList(InetAddress.getByName(dns));
			} catch (UnknownHostException ignored) {
				LOGGER.error(ignored.getLocalizedMessage());
			}
		}

		// If all external DNS resolvers fail, fallback to system DNS
		return Dns.SYSTEM.lookup(hostname);
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

		int attempts = 0;
		while (attempts < MAX_RETRIES) {
			try (Response response = builder.build().newCall(request).execute()) {
				LOGGER.info("URL: " + url);
				LOGGER.info("Response Code: " + response.code());
				if (response.body() != null) {
					LOGGER.info("Response Body: " + response.body().string());
				}
				return Objects.requireNonNull(response.body()).string();
			} catch (IOException e) {
				LOGGER.error("Request failed for: " + url + " -> " + e.getMessage());
				attempts++;
				if (attempts < MAX_RETRIES) {
					LOGGER.info("Retrying in " + RETRY_DELAY_MS / 1000 + " seconds...");
					try {
						TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
					} catch (InterruptedException ignored) {
						LOGGER.error(ignored.getLocalizedMessage());
					}
				} else {
					LOGGER.info("Max retries reached. Request failed for: " + url);
				}
			}
		}
        return null;
    }

	public Map<String, List<String>> requestHeaders(String url) throws IOException {
		LOGGER.info("Requesting headers for URL -> "+url);
		Request request = generateRequest(url);

		int attempts = 0;
		while (attempts < MAX_RETRIES) {
			try (Response response = builder.build().newCall(request).execute()) {
				LOGGER.info("URL: " + url);
				LOGGER.info("Response Code: " + response.code());
				if (response.body() != null) {
					LOGGER.info("Response Body: " + response.headers().toMultimap());
				}
				return Objects.requireNonNull(response.headers()).toMultimap();
			} catch (IOException e) {
				LOGGER.error("Request failed for: " + url + " -> " + e.getMessage());
				attempts++;
				if (attempts < MAX_RETRIES) {
					LOGGER.info("Retrying in " + RETRY_DELAY_MS / 1000 + " seconds...");
					try {
						TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
					} catch (InterruptedException ignored) {
						LOGGER.error(ignored.getLocalizedMessage());
					}
				} else {
					LOGGER.info("Max retries reached. Request failed for: " + url);
				}
			}
		}
		return null;
	}

	public Map<String, List<String>> requestHeadersUsingHttpsURLConnection(String url) throws IOException {
		LOGGER.info("Requesting headers for URL -> " + url);
		URL requestUrl = new URL(url);
		HttpsURLConnection connection = (HttpsURLConnection) requestUrl.openConnection();

		connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36");
		connection.setRequestProperty("Accept", "application/json, text/plain, */*");
		connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
		connection.setRequestProperty("Connection", "keep-alive");
		connection.setRequestProperty("Host", getHostFromUrl(url)); // Extracts Host dynamically

		try {
			return connection.getHeaderFields();
		} finally {
			connection.disconnect();
		}
	}

	public String getContentType(String url) throws IOException {
		LOGGER.info("Requesting content type for URL -> "+url);
		Request request = generateRequestHead(url);

		int attempts = 0;
		while (attempts < MAX_RETRIES) {
			try (Response response = builder.build().newCall(request).execute()) {
				LOGGER.info("URL: " + url);
				LOGGER.info("Response Code: " + response.code());
				if (response.body() != null) {
					LOGGER.info("Response Body: " + response.body().contentType().toString());
				}
				return Objects.requireNonNull(response.body()).contentType().toString();
			} catch (IOException e) {
				LOGGER.error("Request failed for: " + url + " -> " + e.getMessage());
				attempts++;
				if (attempts < MAX_RETRIES) {
					LOGGER.info("Retrying in " + RETRY_DELAY_MS / 1000 + " seconds...");
					try {
						TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
					} catch (InterruptedException ignored) {
						LOGGER.error(ignored.getLocalizedMessage());
					}
				} else {
					LOGGER.info("Max retries reached. Request failed for: " + url);
				}
			}
		}
		return null;
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