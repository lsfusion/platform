package lsfusion.server.logics.form.stat.struct.imports.hierarchy.json;

import lsfusion.base.ReflectionUtils;
import lsfusion.base.file.RawFileData;
import org.apache.commons.io.input.BOMInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.URL;
import java.util.Iterator;

public class JSONReader {

    public static Object readRootObject(RawFileData file, String root, String charset) throws IOException, JSONException {
        Object rootNode = JSONReader.readObject(file, charset);
        if (root != null) {
            rootNode = JSONReader.findRootNode(rootNode, null, root);
            if (rootNode == null)
                throw new RuntimeException(String.format("Import JSON error: root node %s not found", root));
        }
        return rootNode;
    }

    public static void writeRootObject(Object object, PrintWriter printWriter) throws JSONException {
        writeObject(object, printWriter);
    }

    public static Object readObject(RawFileData file, String charset) throws IOException, JSONException {
        try (InputStream is = file.getInputStream()) {
            final BufferedReader rd = new BufferedReader(new InputStreamReader(new BOMInputStream(is), charset));
            return readObject(rd);
        }
    }

    public static Object readObject(Reader r) throws IOException {
        return readObject(readAll(r));
    }

    public static Object readObject(String s) {
        return new JSONTokener(s).nextValue();
    }

    public static void writeObject(Object object, Writer w) {
        ReflectionUtils.invokePrivateMethod(JSONObject.class, null, "writeValue", new Class[]{Writer.class, Object.class, int.class, int.class}, w, object, 0, 0);
    }

    public static JSONObject read(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            return new JSONObject(readAll(new BufferedReader(new InputStreamReader(is))));
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
        if (rootNode instanceof JSONArray) {
            JSONArray array = (JSONArray) rootNode;
            for (int i = 0; i < array.length(); i++) {
                Object result = findRootNode(array.get(i), null, root);
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
        } else if (rootName != null && rootName.equals(root))
                return rootNode;
        return null;
    }

    public static JSONObject toJSONObject(Object object, boolean convertValue) throws JSONException {
        if(object instanceof JSONObject)
            return (JSONObject) object;

        if(!convertValue)
            return null;
        
        JSONObject virtObject = new JSONObject();
        virtObject.put("value", object);
        return virtObject;
    }

    public static Object fromJSONObject(JSONObject object, boolean convertValue) throws JSONException {
        if(convertValue) {
            Iterator keys = object.keys();
            if (keys.hasNext()) {
                String next = (String) keys.next();
                if (!keys.hasNext() && next.equals("value")) 
                    return object.get(next);
            }
        }
        return object;
    }
}