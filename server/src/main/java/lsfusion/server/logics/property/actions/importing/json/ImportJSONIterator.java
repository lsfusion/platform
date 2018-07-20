package lsfusion.server.logics.property.actions.importing.json;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import org.apache.commons.collections.iterators.SingletonIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.*;

public abstract class ImportJSONIterator extends ImportIterator {
    private final ImOrderSet<LCP> properties;

    private Iterator<JSONObject> childrenIterator;

    public ImportJSONIterator(byte[] file, ImOrderSet<LCP> properties, String root, boolean hasListOption) throws IOException, JSONException {
        this.properties = properties;
        Object json = JSONReader.readObject(file);
        if (root != null) {
            Object rootNode = JSONReader.findRootNode(json, null, root);
            if (rootNode != null) {
                //если hasListOption, то берём сам объект, иначе - его children.
                // Для JSONArray hasListOption не проверяем, так как hasListOption=true не имеет смысла
                childrenIterator = hasListOption && rootNode instanceof JSONObject ? new SingletonIterator(rootNode) : JSONReader.getChildren(rootNode).iterator();
            } else {
                throw new RuntimeException(String.format("Import JSON error: root node %s not found", root));
            }
        } else {
            childrenIterator = hasListOption && json instanceof JSONObject ? new SingletonIterator(json) : JSONReader.getChildren(json).iterator();
        }
    }

    @Override
    public List<String> nextRow() {
        if (childrenIterator.hasNext()) {
            JSONObject child = childrenIterator.next();

            List<String> listRow = new ArrayList<>();
            Map<String, Integer> mapping = new HashMap<>();
            Map<Integer, String> childMapping = new HashMap<>();
            Iterator<String> childIterator = child.keys();
            int i = 0;
            while (childIterator.hasNext()) {
                String key = childIterator.next();
                mapping.put(key, i);
                childMapping.put(i, getStringChild(child, key));
                i++;
            }
            List<Integer> columns = getColumns(mapping);

            for (Integer column : columns) {
                String c = childMapping.get(column);
                listRow.add(formatValue(properties, columns, column, c));
            }
            return listRow;
        }

        return null;
    }

    @Override
    protected void release() {
    }

    public abstract List<Integer> getColumns(Map<String, Integer> mapping);

    private String formatValue(ImOrderSet<LCP> properties, List<Integer> columns, Integer column, String value) {
        DateFormat dateFormat = getDateFormat(properties, columns, column);
        if (dateFormat != null && value != null) {
            value = parseFormatDate(dateFormat, value);
        }
        return value;
    }

    private String getStringChild(JSONObject object, String key) {
        try {
            return object.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }

    private List<JSONObject> getChildren(Object rootNode) throws JSONException {
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
                else if (object instanceof JSONArray) { //array of primitive values
                    for (int i = 0; i < ((JSONArray) object).length(); i++) {
                        Object child = ((JSONArray) object).get(i);
                        if (child instanceof String)
                            result.add(new JSONObject("{" + key + ":" + child + "}"));
                    }
                }
            }
        }
        return result;
    }
}