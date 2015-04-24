package pl.devsite.icestreamer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class SparkTestUtil {

	private int port;

	private HttpClient httpClient;

	public SparkTestUtil(int port) {
		this.port = port;
		Scheme http = new Scheme("http", port, PlainSocketFactory.getSocketFactory());
		SchemeRegistry sr = new SchemeRegistry();
		sr.register(http);
		ClientConnectionManager connMrg = new BasicClientConnectionManager(sr);
		this.httpClient = new DefaultHttpClient(connMrg);
	}

	public UrlResponse doMethod(String requestMethod, String path, String body) throws Exception {
		return doMethod(requestMethod, path, body, "text/html");
	}

	private UrlResponse doMethod(String requestMethod, String path, String body,
			String acceptType) throws Exception {
		return doMethod(requestMethod, path, body, acceptType, null);
	}

	public UrlResponse doMethod(String requestMethod, String path, String body,
			String acceptType, Map<String, String> reqHeaders) throws IOException {
		HttpUriRequest httpRequest = getHttpRequest(requestMethod, path, body, acceptType, reqHeaders);
		HttpResponse httpResponse = httpClient.execute(httpRequest);

		UrlResponse urlResponse = new UrlResponse();
		urlResponse.status = httpResponse.getStatusLine().getStatusCode();
		HttpEntity entity = httpResponse.getEntity();
		if (entity != null) {
			urlResponse.body = EntityUtils.toString(entity);
		} else {
			urlResponse.body = "";
		}
		Map<String, String> headers = new HashMap<String, String>();
		Header[] allHeaders = httpResponse.getAllHeaders();
		for (Header header : allHeaders) {
			headers.put(header.getName(), header.getValue());
		}
		urlResponse.headers = headers;
		return urlResponse;
	}

	private HttpUriRequest getHttpRequest(String requestMethod, String path, String body,
			String acceptType, Map<String, String> reqHeaders) {
		String uri = "http" + "://localhost:" + port + path;

		if (requestMethod.equals("GET")) {
			HttpGet httpGet = new HttpGet(uri);
			httpGet.setHeader("Accept", acceptType);
			addHeaders(reqHeaders, httpGet);
			return httpGet;
		}

		if (requestMethod.equals("POST")) {
			HttpPost httpPost = new HttpPost(uri);
			httpPost.setHeader("Accept", acceptType);
			addHeaders(reqHeaders, httpPost);
			httpPost.setEntity(new StringEntity(body, "UTF-8"));
			return httpPost;
		}

		if (requestMethod.equals("PATCH")) {
			HttpPatch httpPatch = new HttpPatch(uri);
			httpPatch.setHeader("Accept", acceptType);
			addHeaders(reqHeaders, httpPatch);
			httpPatch.setEntity(new StringEntity(body, "UTF-8"));
			return httpPatch;
		}

		if (requestMethod.equals("DELETE")) {
			HttpDelete httpDelete = new HttpDelete(uri);
			addHeaders(reqHeaders, httpDelete);
			httpDelete.setHeader("Accept", acceptType);
			return httpDelete;
		}

		if (requestMethod.equals("PUT")) {
			HttpPut httpPut = new HttpPut(uri);
			httpPut.setHeader("Accept", acceptType);
			addHeaders(reqHeaders, httpPut);
			httpPut.setEntity(new StringEntity(body, "UTF-8"));
			return httpPut;
		}

		if (requestMethod.equals("HEAD")) {
			HttpHead httpHead = new HttpHead(uri);
			addHeaders(reqHeaders, httpHead);
			return httpHead;
		}

		if (requestMethod.equals("TRACE")) {
			HttpTrace httpTrace = new HttpTrace(uri);
			addHeaders(reqHeaders, httpTrace);
			return httpTrace;
		}

		if (requestMethod.equals("OPTIONS")) {
			HttpOptions httpOptions = new HttpOptions(uri);
			addHeaders(reqHeaders, httpOptions);
			return httpOptions;
		}

		throw new IllegalArgumentException("Unknown method " + requestMethod);
	}

	private void addHeaders(Map<String, String> reqHeaders, HttpRequest req) {
		if (reqHeaders != null) {
			for (Map.Entry<String, String> header : reqHeaders.entrySet()) {
				req.addHeader(header.getKey(), header.getValue());
			}
		}
	}

	public int getPort() {
		return port;
	}

	public static class UrlResponse {

		public Map<String, String> headers;
		public String body;
		public int status;
	}

	public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (Exception e) {
		}
	}

}
