package com.naholyr.android.horairessncf.ws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

class JSONWebServiceClient {

	public JSONResponse query(String serverUrl, Map<String, Object> params) throws IOException, MalformedURLException, JSONException {
		String queryParams = "";
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			queryParams += queryParams.equals("") ? "?" : "&";
			queryParams += URLEncoder.encode(entry.getKey()) + "=" + URLEncoder.encode(String.valueOf(entry.getValue()));
		}
		URL url = new URL(serverUrl + queryParams);
		InputStream is = (InputStream) url.getContent();

		if (is == null) {
			throw new IOException("Erreur au chargement du service " + url.toString());
		}

		StringWriter writer = new StringWriter();
		BufferedReader buffer = new BufferedReader(new InputStreamReader(is));
		String line = "";
		while (null != (line = buffer.readLine())) {
			writer.write(line);
		}

		return new JSONResponse(writer.toString());
	}

	public static final class JSONResponse {

		private JSONObject mObject;

		public JSONResponse(String json) throws JSONException {
			mObject = new JSONObject(json);
		}

		public boolean isSuccess() {
			return mObject.has("success");
		}

		public JSONObject getSuccessData() {
			if (isSuccess()) {
				try {
					return mObject.getJSONObject("success");
				} catch (JSONException e) {
					return null;
				}
			} else {
				return null;
			}
		}

		public boolean isError() {
			return mObject.has("error");
		}

		public JSONObject getErrorData() {
			if (isError()) {
				try {
					return mObject.getJSONObject("info");
				} catch (JSONException e) {
					return null;
				}
			} else {
				return null;
			}
		}

		public String getErrorMessage() {
			return getErrorMessage(true);
		}

		public String getErrorMessage(boolean withCode) {
			if (isError()) {
				try {
					return mObject.getInt("code") + " - " + mObject.getString("error");
				} catch (JSONException e) {
					return null;
				}
			} else {
				return null;
			}
		}

		public int getCode() {
			try {
				return mObject.getInt("code");
			} catch (JSONException e) {
				return 0;
			}
		}

	}

}
