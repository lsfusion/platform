package lsfusion.server.logics.property.actions.importing.json;

import lsfusion.base.Pair;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class ImportFormJSONIterator extends ImportFormIterator {

    private JSONObject objectRoot;
    private Iterator<String> objectIterator;

    private JSONArray arrayRoot;
    private String arrayKey;
    private int i;

    ImportFormJSONIterator(Pair<String, Object> keyValueRoot) {
        if(keyValueRoot.second instanceof JSONObject) {
            this.objectRoot = (JSONObject) keyValueRoot.second;
            objectIterator = ((JSONObject)keyValueRoot.second).keys();
        } else if(keyValueRoot.second instanceof JSONArray) {
            this.arrayRoot = (JSONArray) keyValueRoot.second;
            this.arrayKey = keyValueRoot.first;
            this.i = 0;
        }

    }

    @Override
    public boolean hasNext() {
        if (objectRoot != null)
            return objectIterator.hasNext();
        else
            return arrayRoot != null && arrayRoot.length() > i;
    }

    @Override
    public Pair<String, Object> next() {
        try {
            if (objectRoot != null) {
                String key = objectIterator.next();
                Object value = getChild(objectRoot, key);
                return Pair.create(key, value);
            } else if (arrayRoot != null) {
                Pair<String, Object> entry = Pair.create(arrayKey, arrayRoot.get(i));
                i++;
                return entry;
            }
        } catch (JSONException ignored) {
        }
        return null;
    }

    @Override
    public void remove() {
    }

    private Object getChild(JSONObject object, String key) {
        try {
            return object.get(key);
        } catch (JSONException e) {
            return null;
        }
    }
}