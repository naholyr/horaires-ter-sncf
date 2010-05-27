package com.naholyr.android.horairessncf.termobile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.naholyr.android.horairessncf.Util;

public class HTTP {

	public static final int BUFFER_SIZE = 1024;

	public static final class Response {

		private Map<String, List<String>> headers = new HashMap<String, List<String>>();
		private String body = null;
		private Integer statusCode = null;
		private String statusDescription = null;

		private static final Pattern PATTERN_STATUS = Pattern.compile("^[A-Z/0-9\\.]+ ([0-9]+) (.*)$");

		public Response(BufferedReader r) throws IOException {
			this(r, true);
		}

		public Response(BufferedReader r, boolean readBody) throws IOException {
			body = null;
			String line;

			// Read and parse headers, line by line
			boolean parsedStatus = false;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (line.equals("")) {
					// End of headers
					break;
				} else {
					if (!parsedStatus) {
						Matcher m = PATTERN_STATUS.matcher(line);
						parsedStatus = true;
						if (m.find()) {
							statusCode = Integer.parseInt(m.group(1));
							statusDescription = m.group(2);
							continue;
						} else {
							statusCode = 0;
							statusDescription = null;
						}
					}
					if (line.indexOf(':') != -1) {
						addHeader(line);
					}
				}
			}

			// Read body, all at once if the content-length header has been
			// provided, else line by line
			if (readBody) {
				if (headers.containsKey("content-length")) {
					try {
						// Retrieve content-length
						int contentLength = Integer.parseInt(getHeader("content-length"));
						// Read all at once
						char[] buffer = new char[contentLength];
						r.read(buffer, 0, contentLength);
						body = new String(buffer);
					} catch (NumberFormatException e) {
						// Mark body as unread
						body = null;
					}
				}
				if (body == null) {
					// Read line by line
					while ((line = r.readLine()) != null) {
						line = line.trim();
						if (!line.equals("")) {
							body += line + "\n";
						}
					}
				}
			}

			r.close();
		}

		public void addHeader(String line) {
			int sepPos = line.indexOf(':');
			String headerName = line.substring(0, sepPos).trim().toLowerCase();
			String headerValue = line.substring(sepPos + 1).trim();
			if (!headers.containsKey(headerName)) {
				headers.put(headerName, new ArrayList<String>());
			}
			headers.get(headerName).add(headerValue);
		}

		public String getBody() {
			return body;
		}

		public List<String> getHeaderList(String name) {
			if (headers.containsKey(name)) {
				return headers.get(name);
			}
			return null;
		}

		public String getHeader(String name, int index) {
			if (headers.containsKey(name)) {
				List<String> values = headers.get(name);
				if (0 <= index && index < values.size()) {
					return values.get(index);
				}
			}
			return null;
		}

		public String getHeader(String name) {
			return getHeader(name, 0);
		}

		public String[] getHeaderNames() {
			return headers.keySet().toArray(new String[0]);
		}

		public Integer getStatusCode() {
			return statusCode;
		}

		public String getStatusDescription() {
			return statusDescription;
		}

	}

	public static final class Request {

		public static Map<String, String> getDefaultHeaders(String charset) {
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3");
			headers.put("Accept", "text/html");
			headers.put("Accept-Language", "fr");
			headers.put("Accept-Charset", charset);
			headers.put("Keep-Alive", "300");
			// headers.put("Cache-Control", "max-age=0");
			headers.put("Connection", "keep-alive");

			return headers;
		}

		public static Response GET(String host, int port, String path, String charset) throws IOException {
			return GET(host, port, path, getDefaultHeaders(charset), charset);
		}

		public static Response GET(String host, int port, String path, boolean readBody, String charset) throws IOException {
			return GET(host, port, path, getDefaultHeaders(charset), readBody, charset);
		}

		public static Response GET(String host, int port, String path, Map<String, String> headers, String charset) throws IOException {
			return request("GET", host, port, path, headers, charset);
		}

		public static Response GET(String host, int port, String path, Map<String, String> headers, boolean readBody, String charset) throws IOException {
			return request("GET", host, port, path, headers, readBody, charset);
		}

		public static Response POST(String host, int port, String path, Map<String, String> data, String charset) throws IOException {
			return POST(host, port, path, getDefaultHeaders(charset), data, charset);
		}

		public static Response POST(String host, int port, String path, Map<String, String> data, boolean readBody, String charset) throws IOException {
			return POST(host, port, path, getDefaultHeaders(charset), data, readBody, charset);
		}

		private static String getPostContent(Map<String, String> data, String charset) {
			String content = "";
			boolean addAmp = false;
			for (Map.Entry<String, String> item : data.entrySet()) {
				if (addAmp) {
					content += "&";
				}
				String key = item.getKey();
				String value = Util.removeAccents(item.getValue());
				content += URLEncoder.encode(key) + "=" + URLEncoder.encode(value);
				if (!addAmp) {
					addAmp = true;
				}
			}

			return content;
		}

		public static Response POST(String host, int port, String path, Map<String, String> headers, Map<String, String> data, String charset) throws IOException {
			headers.put("Content-Type", "application/x-www-form-urlencoded");
			return request("POST", host, port, path, headers, getPostContent(data, charset), charset);
		}

		public static Response POST(String host, int port, String path, Map<String, String> headers, Map<String, String> data, boolean readBody, String charset) throws IOException {
			headers.put("Content-Type", "application/x-www-form-urlencoded");
			return request("POST", host, port, path, headers, getPostContent(data, charset), readBody, charset);
		}

		public static Response request(String method, String host, int port, String path, Map<String, String> headers, String charset) throws IOException {
			return request(method, host, port, path, headers, (String) null, charset);
		}

		public static Response request(String method, String host, int port, String path, Map<String, String> headers, boolean readBody, String charset) throws IOException {
			return request(method, host, port, path, headers, (String) null, readBody, charset);
		}

		public static Response request(String method, String host, int port, String path, Map<String, String> headers, String content, String charset) throws IOException {
			return request(method, host, port, path, headers, content, true, charset);
		}

		public static Response request(String method, String host, int port, String path, Map<String, String> headers, String content, boolean readBody, String charset)
				throws IOException {
			Socket s = new Socket(host, port);
			s.setSoTimeout(Util.READ_URL_SOCKET_TIMEOUT);
			OutputStreamWriter w = new OutputStreamWriter(s.getOutputStream());
			BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), charset), BUFFER_SIZE);
			w.write(method + " " + path + " HTTP/1.1\n");
			w.write("Host: " + host + "\n");
			if (content != null) {
				headers.put("Content-Length", String.valueOf(content.length()));
			}
			for (Map.Entry<String, String> header : headers.entrySet()) {
				String name = header.getKey();
				String value = header.getValue();
				String headerLine = name + ": " + value + "\n";
				w.write(headerLine);
			}
			w.write("\n");
			if (content != null) {
				w.write(content);
				w.write("\n");
			}
			w.flush();

			long time1 = Calendar.getInstance().getTimeInMillis();
			while (!r.ready()) {
				long time2 = Calendar.getInstance().getTimeInMillis();
				long delay = time2 - time1;
				if (delay < Util.READ_URL_READY_TIMEOUT) {
					continue;
				} else {
					throw new IOException("La requête a échoué : Le serveur a mis trop de temps à répondre.");
				}
			}

			Response response = new Response(r, readBody);

			w.close();

			return response;
		}

	}

}
