package lsfusion.server.logics.property.actions.importing.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public static Object findRootNode(Object rootNode, String rootName, String root) throws JSONException {
        if (rootName != null && rootName.equals(root))
            return rootNode;

        if (rootNode instanceof JSONArray) {

            for (JSONObject child : getChildren(rootNode)) {
                Object result = findRootNode(child, null, root);
                if (result != null)
                    return result;
            }

        } else if (rootNode instanceof JSONObject) {

            Iterator<String> it = ((JSONObject) rootNode).keys();
            while (it.hasNext()) {
                String key = it.next();
                Object child = ((JSONObject) rootNode).get(key);
                Object result = findRootNode(child, key, root);
                if(result != null)
                    return result;
            }

        }
        return null;
    }

    public static List<JSONObject> getChildren(Object rootNode) throws JSONException {
        List<JSONObject> result = new ArrayList<>();
        if (rootNode instanceof JSONArray) {
            for (int i = 0; i < ((JSONArray) rootNode).length(); i++) {
                Object child = ((JSONArray) rootNode).get(i);
                if (child instanceof JSONObject)
                    result.add((JSONObject) child);
            }
            return result;
        } else {
            Iterator<String> objectIterator = ((JSONObject) rootNode).keys();
            while (objectIterator.hasNext()) {
                String key = objectIterator.next();
                Object object = ((JSONObject) rootNode).get(key);
                if (object instanceof JSONObject)
                    result.add((JSONObject) object);
            }
        }
        return result;
    }
}