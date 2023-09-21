package org.epos_ip.tcsconnector.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import javax.net.ssl.HttpsURLConnection;

import org.epos.core.ExternalServicesRequest;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class URLExecuteTest {

	public static void main(String[] args) throws IOException {

		String template1 = "https://nfocrl.u-strasbg.fr/fdsnws/event/1/query?starttime=2016-01-01T00%3A00%3A00Z&endtime=2016-01-01T05%3A00%3A00Z&minlatitude=38&maxlatitude=38.65&minlongitude=21.35&maxlongitude=22.5&mindepth=0&maxdepth=20&minmagnitude=1&maxmagnitude=7&includeallorigins=false&includeallmagnitudes=false&includearrivals=false&limit=30&orderby=time&format=xml";

		Map<String, Object> response = null;
		try {
			System.out.println("normal");
			response = ExternalServicesRequest.getInstance().getRedirect(template1);
		}catch(javax.net.ssl.SSLException e) {
			System.err.println(e.getLocalizedMessage());
			System.out.println("legacy");
			response = ExternalServicesRequest.getInstance().getLegacyRedirect(template1);
		}

		System.out.println(response.toString());

		//System.out.println(ExternalServicesRequest.getInstance().requestPayload(template1));

		/*URL url;
		try {

			HttpsURLConnection con;
			url = new URL(template1); 
			con = (HttpsURLConnection) url.openConnection();
			BufferedReader br = null;
			if (100 <= con.getResponseCode() && con.getResponseCode() <= 399) {
			    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			} else {
			    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
			}

			String responseBody = br.lines().collect(Collectors.joining());

			System.out.println(responseBody);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/


	}

}
