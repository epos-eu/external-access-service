package org.epos.api.beans;

public class ErrorMessage {
	
	private String httpCode;
	private String message;
	private String url;
	private String contentType;
	
	public ErrorMessage(String httpCode, String message, String url, String contentType) {
		super();
		this.httpCode = httpCode;
		this.message = message;
		this.url = url;
		this.contentType = contentType;
	}

	public ErrorMessage() {
	}

	public String getHttpCode() {
		return httpCode;
	}

	public void setHttpCode(String httpCode) {
		this.httpCode = httpCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Override
	public String toString() {
		return "ErrorMessage [httpCode=" + httpCode + ", message=" + message + ", url=" + url + ", contentType="
				+ contentType + "]";
	}
	
}