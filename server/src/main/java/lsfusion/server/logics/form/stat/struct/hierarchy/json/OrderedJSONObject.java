package lsfusion.server.logics.form.stat.struct.hierarchy.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// JSONObject keeps its entries in a HashMap and gives no way to replace it, so all the methods that read that map directly are overridden to use an ordered one
// (the rest of JSONObject, write / toString included, goes through these methods)
public class OrderedJSONObject extends JSONObject {

    private final Map<String, Object> map = new LinkedHashMap<>();

    @Override
    public JSONObject put(String key, Object value) throws JSONException {
        if (key == null)
            throw new NullPointerException("Null key.");
        if (value != null) {
            testValidity(value);
            map.put(key, value);
        } else
            map.remove(key);
        return this;
    }

    @Override
    public Object opt(String key) {
        return key == null ? null : map.get(key);
    }

    @Override
    public boolean has(String key) {
        return map.containsKey(key);
    }

    @Override
    public Object remove(String key) {
        return map.remove(key);
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    protected Set<Map.Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    @Override
    public int length() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public JSONArray names() {
        return map.isEmpty() ? null : new JSONArray(map.keySet());
    }

    @Override
    public Class<? extends Map> getMapType() {
        return map.getClass();
    }
}
