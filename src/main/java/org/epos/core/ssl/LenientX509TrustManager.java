/*******************************************************************************
 * Copyright 2021 EPOS ERIC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.epos.core.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks X509 certificates with a CA root but does not check those certificates that are self-signed (but does log these at ERROR level)
 *
 */
public class LenientX509TrustManager implements X509TrustManager {
	
	private static final Logger LOG = LoggerFactory.getLogger(LenientX509TrustManager.class);
	
	private X509TrustManager delegate;

	private  LenientX509TrustManager(X509TrustManager delegate) {
		this.delegate = delegate;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		delegate.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (isSelfSigned(chain)) {
        	if (LOG.isErrorEnabled()) {
        		X509Certificate endCert = chain[chain.length-1];	
        		LOG.error(String.format(
        				"Self-signed certificate detected (%s). Note that this has been accepted but was not validated", 
        				endCert.getSubjectX500Principal().getName()));
        	}        	
            return;
        }
        delegate.checkServerTrusted(chain, authType);
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return delegate.getAcceptedIssuers();
	}
	
    private boolean isSelfSigned(X509Certificate[] chain) 
    {
    	if (chain.length == 1) {
    		return true;
    	}
    	else if (chain.length > 1) {
    		X509Certificate endCert = chain[chain.length-1];
    		X500Principal issuer = endCert.getIssuerX500Principal();
    		X500Principal subject = endCert.getSubjectX500Principal();
    		if (issuer.getName().equals(subject.getName())) {
    			return true;
    		}
    	}
    	return false;
    }

    public static X509TrustManager[] wrap(X509TrustManager delegate) {
        return new X509TrustManager[]{new LenientX509TrustManager(delegate)};
    }
}
