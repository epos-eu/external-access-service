package org.epos_ip.tcsconnector.core;

import java.io.IOException;

import org.epos.core.ExternalServicesRequest;

public class RequestTest {
	
	public static void main(String[] args) {
		try {
			ExternalServicesRequest.getInstance().requestPayload("https://insar.irea.cnr.it/geoserver/gwc/service/wmts");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
