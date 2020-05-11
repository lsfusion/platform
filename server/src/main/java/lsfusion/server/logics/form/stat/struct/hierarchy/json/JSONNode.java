package lsfusion.server.logics.form.stat.struct.hierarchy.json;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.hierarchy.Node;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.json.JSONReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class JSONNode implements Node<JSONNode> {
    public final JSONObject element;

    public JSONNode(JSONObject element) {
        this.element = element;
    }
    
    public static JSONNode getJSONNode(Object object, boolean convertValue) throws JSONException {
        JSONObject jsonObject = JSONReader.toJSONObject(object, convertValue);
        if(jsonObject == null) {
            assert !convertValue;
            return null;
        }
        return new JSONNode(jsonObject);
    }

    public static Object putJSONNode(JSONNode node, boolean convertValue) throws JSONException {
        return JSONReader.fromJSONObject(node.element, convertValue);
    }

    @Override
    public JSONNode getNode(String key) {
        try {
            Object childElement = element.opt(key);
            if(childElement == null)
                return null;
            return getJSONNode(childElement, false); // no need to convert value for property group
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        }
    }

    public void addNode(JSONNode node, String key, JSONNode childNode) {
        try {
            node.element.put(key, putJSONNode(childNode, false)); // no need to convert value for property group
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        }
    }

    public void removeNode(JSONNode node, JSONNode childNode) { 
        assert !isUpDown();
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getValue(String key, boolean attr, Type type) throws ParseException {
        try {
            Object value = element.opt(key);
//            if(value instanceof JSONArray || value instanceof JSONObject) // if incorrect structure just consider it missing
//                value = null;
            return type.parseJSON(value);
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        } catch (ParseException e) {
            throw new ParseException(e.getMessage() + String.format(" (tag %s)", key), e);
        }
    }

    @Override
    public Iterable<Pair<Object, JSONNode>> getMap(String key, boolean isIndex) {
        try {
            MList<Pair<Object, JSONNode>> mResult = ListFact.mList();
            Object child = element.opt(key);
            if(child != null) {
                if (isIndex) {
                    JSONArray array = (JSONArray) child;
                    for (int i = 0, size = array.length(); i < size; i++)
                        mResult.add(new Pair<>(i, getJSONNode(array.get(i), true)));
                } else {
                    JSONObject object = (JSONObject) child;
                    for (Iterator it = object.keys(); it.hasNext(); ) {
                        String objectKey = (String) it.next();
                        mResult.add(new Pair<>(objectKey, getJSONNode(object.get(objectKey), true)));
                    }
                }
            }
            return mResult.immutableList();
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public JSONNode createNode() {
        return new JSONNode(new JSONObject());
    }

    @Override
    public boolean isUpDown() {
        return false;
    }

    public void addValue(JSONNode node, String key, boolean attr, Object value, Type type) {
        try {
            node.element.put(key, parseObject(type.formatJSON(value)));
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        }
    }

    //check if it's json inside
    private Object parseObject(Object obj) {
        try {
            if (obj instanceof String) {
                return JSONReader.readObject((String) obj);
            }
        } catch (Exception ignored) {
        }
        return obj;
    }

    public boolean addMap(JSONNode node, String key, boolean isIndex, Iterable<Pair<Object, JSONNode>> map) {
        try {
            Object addObject;
            if(isIndex) {
                JSONArray array = new JSONArray();
                for(Pair<Object, JSONNode> value : map)
                    array.put(putJSONNode(value.second, true));
                addObject = array;
            } else {
                JSONObject object = new JSONObject();
                for(Pair<Object, JSONNode> value : map)
                    object.put((String) value.first, putJSONNode(value.second, true));
                addObject = object;
            }                
            node.element.put(key, addObject);
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        }        
        return true;
    }
}
