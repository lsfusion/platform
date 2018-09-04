package lsfusion.server.logics.property.actions.importing.json;

import lsfusion.base.Pair;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ImportFormJSONIterator extends ImportFormIterator {

    private Iterator<Pair<String, Object>> childrenIterator;

    ImportFormJSONIterator(Pair<String, Object> keyValueRoot, Map<String, List<List<String>>> formObjectGroups, Map<String, List<List<String>>> formPropertyGroups) {

        List<Pair<String, Object>> children = new ArrayList<>();

        if (keyValueRoot.second instanceof JSONObject) {
            Iterator objectIterator = ((JSONObject) keyValueRoot.second).keys();
            while (objectIterator.hasNext()) {
                String key = (String) objectIterator.next();
                Object object = getChild((JSONObject) keyValueRoot.second, key);
                if (object instanceof JSONObject) {
                    List<Pair<String, Object>> subChildren = getSubChildren(formObjectGroups, formPropertyGroups, key, (JSONObject) object);
                    if(subChildren != null) {
                        children.addAll(subChildren);
                    } else {
                        children.add(Pair.create(key, object));
                    }
                } else
                    children.add(Pair.create(key, object));
            }
        } else if (keyValueRoot.second instanceof JSONArray) {
                for (int i = 0; i < ((JSONArray) keyValueRoot.second).length(); i++) {
                    Object child = getChild((JSONArray) keyValueRoot.second, i);
                    children.add(Pair.create(keyValueRoot.first, child));
                }
        }
        this.childrenIterator = children.iterator();
    }

    @Override
    public boolean hasNext() {
        return childrenIterator.hasNext();
    }

    @Override
    public Pair<String, Object> next() {
        return childrenIterator.next();
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

    private Object getChild(JSONArray object, int i) {
        try {
            return object.get(i);
        } catch (JSONException e) {
            return null;
        }
    }

    private List<Pair<String, Object>> getSubChildren(Map<String, List<List<String>>> formObjectGroups, Map<String, List<List<String>>> formPropertyGroups, String key, JSONObject child) {
        //possible trouble if object and property has equal names and not equal groups
        List<Pair<String, Object>> result = null;
        List<List<String>> formObjectGroupList = formObjectGroups.get(key);
        if (formObjectGroupList != null) {
            for (List<String> formObjectGroupEntry : formObjectGroupList) {
                List<Pair<String, Object>> subChildren = null;
                //order is important
                for (int i = 0; i < formObjectGroupEntry.size(); i++) {
                    List<Pair<String, Object>> currentSubChildren = new ArrayList<>();
                    String subGroup = formObjectGroupEntry.get(i);
                    if (subChildren == null) {
                        currentSubChildren.addAll(getChildren(child, subGroup));
                    } else {
                        for (Object subChild : subChildren) {
                            currentSubChildren.addAll(getChildren((JSONObject) subChild, subGroup));
                        }
                    }
                    subChildren = currentSubChildren;
                }
                if (result == null)
                    result = new ArrayList<>();
                if (subChildren != null)
                    result.addAll(subChildren);
            }
        } else {
            List<List<String>> formPropertyGroupList = formPropertyGroups.get(key);
            if (formPropertyGroupList != null) {
                for (List<String> formPropertyGroupEntry : formPropertyGroupList) {
                    List<Pair<String, Object>> subChildren = null;
                    //order is important
                    for (int i = 0; i < formPropertyGroupEntry.size(); i++) {
                        List<Pair<String, Object>> currentSubChildren = new ArrayList<>();
                        String subGroup = formPropertyGroupEntry.get(i);
                        if (subChildren == null) {
                            currentSubChildren.addAll(getChildren(child, subGroup));
                        } else {
                            for (Pair<String, Object> subChild : subChildren) {
                                if(subChild.second instanceof JSONObject) {
                                    currentSubChildren.addAll(getChildren((JSONObject) subChild.second, subGroup));
                                }
                            }
                        }
                        subChildren = currentSubChildren;
                    }
                    if (result == null)
                        result = new ArrayList<>();
                    if (subChildren != null)
                        result.addAll(subChildren);
                }
            }
        }
        return result;
    }

    private List<Pair<String, Object>> getChildren(JSONObject parent, String childName) {
        List<Pair<String, Object>> result = new ArrayList<>();
        Iterator objectIterator = parent.keys();
        while (objectIterator.hasNext()) {
            String key = (String) objectIterator.next();
            Object object = getChild(parent, key);
            if (key.equals(childName)) {
                if (object instanceof JSONObject)
                    result.add(Pair.create(key, object));
                else if (object instanceof JSONArray) {
                    for (int i = 0; i < ((JSONArray) object).length(); i++) {
                        try {
                            Object child = ((JSONArray) object).get(i);
                            if (child instanceof JSONObject)
                                result.add(Pair.create(key, child));
                        } catch (JSONException ignored) {
                        }
                    }
                } else if(object instanceof String)
                    result.add(Pair.create(key, object));
            }

        }
        return result;
    }
}