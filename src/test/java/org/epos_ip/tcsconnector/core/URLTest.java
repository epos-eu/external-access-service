package org.epos_ip.tcsconnector.core;

import java.net.URISyntaxException;
import java.util.HashMap;
import org.epos.core.URLGeneration;

public class URLTest {

	public static void main(String[] args) {
		
		String template1 = "https://opgc.fr/vobs/rest2/req.php/voldorad/spectra/{startDate}/{endDate}?test{&test=test,prova=prova}";
		
		HashMap<String, Object> parameters = new HashMap<>();
		parameters.put("startDate","2010-05-08T10:00");
		parameters.put("endDate","2010-05-12T10:00");
		parameters.put("test=test","goal");
		
		String compiledUrl = null;
		compiledUrl = URLGeneration.generateURLFromTemplateAndMap(template1, parameters);
		
		System.out.println("URL Prerefined:" + compiledUrl);
		
		try {
			if (URLGeneration.removeQueryParameter(compiledUrl, null) != null)
				compiledUrl = URLGeneration.removeQueryParameter(compiledUrl, null);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		System.out.println("Final URL \n"+compiledUrl);
		
	}

}
