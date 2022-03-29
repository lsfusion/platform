package lsfusion.server.logics.form.stat.struct.hierarchy.json;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

public class OrderedJSONObject extends JSONObject {

    //If there will be problems using reflection, possible to do as in com.nimbusds.oauth2.sdk.util.OrderedJSONObject
    public OrderedJSONObject() {
        super();
        //hashmap -> linkedhashmap
        try {
            Field field = JSONObject.class.getDeclaredField("map");
            field.setAccessible(true);
            field.set(this, new LinkedHashMap<>());
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }
}
