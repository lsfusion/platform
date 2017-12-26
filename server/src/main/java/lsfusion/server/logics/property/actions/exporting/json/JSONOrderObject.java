//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lsfusion.server.logics.property.actions.exporting.json;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

public class JSONOrderObject extends JSONObject {

    public JSONOrderObject() {
        super();
        //hashmap -> linkedhashmap
        try {
            Field field = JSONObject.class.getDeclaredField("map");
            field.setAccessible(true);
            field.set(this, new LinkedHashMap<>());
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            System.out.print(ignored);
        }
    }
}