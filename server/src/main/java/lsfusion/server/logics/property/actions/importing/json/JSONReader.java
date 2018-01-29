package lsfusion.server.logics.property.actions.importing.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;

public class JSONReader {

    public static JSONObject read(byte[] file) throws IOException, JSONException {
        try (InputStream is = new ByteArrayInputStream(file)) {
            final BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            final String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    public static Object readObject(byte[] file) throws IOException, JSONException {
        try (InputStream is = new ByteArrayInputStream(file)) {
            final BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            final String jsonText = readAll(rd);
            return jsonText.startsWith("[") ? new JSONArray(jsonText) : new JSONObject(jsonText);
        }
    }

    private static String readAll(final Reader rd) throws IOException {
        final StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}