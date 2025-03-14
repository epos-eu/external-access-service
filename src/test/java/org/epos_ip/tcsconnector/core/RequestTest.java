package org.epos_ip.tcsconnector.core;

import java.io.IOException;

public class RequestTest {
	
	public static void main(String[] args) {
		try {
			ExternalServicesRequestOLD.getInstance().requestPayload("https://insar.irea.cnr.it/geoserver/gwc/service/wmts");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
