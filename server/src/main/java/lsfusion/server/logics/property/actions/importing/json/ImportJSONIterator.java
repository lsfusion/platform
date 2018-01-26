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
        JSONObject jsonObject = JSONReader.read(file);
        if (root != null) {
            JSONObject rootNode = findRootNode(jsonObject, null, root);
            if (rootNode != null) {
                //если hasListOption, то берём сам объект, иначе - его children.
                childrenIterator = hasListOption ? new SingletonIterator(rootNode) : getChildrenIterator(rootNode);
            } else {
                throw new RuntimeException(String.format("Import JSON error: root node %s not found", root));
            }
        } else {
            childrenIterator = hasListOption ? new SingletonIterator(jsonObject) : getChildrenIterator(jsonObject);
        }
    }

    private JSONObject findRootNode(JSONObject rootNode, String rootName, String root) throws JSONException {
        if (rootName != null && rootName.equals(root))
            return rootNode;
        Iterator<String> it = rootNode.keys();
        while (it.hasNext()) {
            String key = it.next();
            Object child = rootNode.get(key);
            if (child instanceof JSONObject)
                return findRootNode((JSONObject) child, key, root);
            else if (child instanceof JSONArray) {
                for (int i = 0; i < ((JSONArray) child).length(); i++) {
                    Object c = ((JSONArray) child).get(i);
                    if (c instanceof JSONObject)
                        return findRootNode((JSONObject) c, key, root);
                }
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
                String c = getStringChild(child, key);
                mapping.put(key, i);
                childMapping.put(i, c);
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

    private Iterator<JSONObject> getChildrenIterator(JSONObject rootNode) throws JSONException {
        Iterator<String> objectIterator = rootNode.keys();
        List<JSONObject> children = new ArrayList<>();
        while (objectIterator.hasNext()) {
            String key = objectIterator.next();
            children.addAll(getChildren(rootNode, key));
        }
        return children.iterator();
    }

    private List<JSONObject> getChildren(JSONObject objectRoot, String key) throws JSONException {
        Object object = objectRoot.get(key);
        List<JSONObject> result = new ArrayList<>();
        if (object instanceof JSONArray) {
            for (int i = 0; i < ((JSONArray) object).length(); i++) {
                Object child = ((JSONArray) object).get(i);
                if (child instanceof JSONObject)
                    result.add((JSONObject) child);
            }
        } else if (object instanceof JSONObject)
            result.add((JSONObject) object);
        return result;
    }
}