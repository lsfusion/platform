package lsfusion.server.logics.property.actions.importing.json;

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
    private final List<LCP> properties;

    private Iterator<JSONObject> childrenIterator;

    public ImportJSONIterator(byte[] file, List<LCP> properties, String root, boolean hasListOption) throws IOException, JSONException {
        this.properties = properties;
        Object json = JSONReader.readObject(file);
        if (root != null) {
            Object rootNode = findRootNode(json, null, root);
            if (rootNode != null) {
                //если hasListOption, то берём сам объект, иначе - его children.
                // Для JSONArray hasListOption не проверяем, так как hasListOption=true не имеет смысла
                childrenIterator = hasListOption && rootNode instanceof JSONObject ? new SingletonIterator(rootNode) : getChildren(rootNode).iterator();
            } else {
                throw new RuntimeException(String.format("Import JSON error: root node %s not found", root));
            }
        } else {
            childrenIterator = hasListOption && json instanceof JSONObject ? new SingletonIterator(json) : getChildren(json).iterator();
        }
    }

    private Object findRootNode(Object rootNode, String rootName, String root) throws JSONException {
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

    private String formatValue(List<LCP> properties, List<Integer> columns, Integer column, String value) {
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
            }
        }
        return result;
    }
}