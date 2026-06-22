package lsfusion.gwt.client.form.object;

import com.google.gwt.core.client.JavaScriptObject;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeStringMap;

import java.io.Serializable;
import java.util.*;

public class GGroupObjectValue implements Serializable {
    public static final GGroupObjectValue EMPTY = new GGroupObjectValue();

    private static ArrayList<GGroupObjectValue> createEmptyList() {
        ArrayList<GGroupObjectValue> result = new ArrayList<>();
        result.add(EMPTY);
        return result;
    }
    public static final ArrayList<GGroupObjectValue> SINGLE_EMPTY_KEY_LIST = createEmptyList();

    private int size = 0;
    private int[] keys;
    private Serializable[] values;

    private int singleKey;
    private Serializable singleValue;

    public GGroupObjectValue() {
    }

    public GGroupObjectValue(Map<Integer, Serializable> k) {
        int ks = k.size();
        if (ks != 0) {
            if (ks == 1) {
                Map.Entry<Integer, Serializable> e = k.entrySet().iterator().next();
                initSingle(e.getKey(), e.getValue());
            } else {
                size = ks;
                keys = new int[size];
                values = new Serializable[size];

                int i = 0;
                for (Map.Entry<Integer, Serializable> e : k.entrySet()) {
                    keys[i] = e.getKey();
                    values[i++] = e.getValue();
                }
            }
        }
    }

    public GGroupObjectValue(int size, int[] keys, Serializable[] values) {
        if(size != 0) {
            if (size == 1)
                initSingle(keys[0], values[0]);
            else {
                this.size = size;
                this.keys = keys;
                this.values = values;
            }
        }
    }

    public GGroupObjectValue(int key, Serializable value) {
        initSingle(key, value);
    }

    public static GGroupObjectValue getFullKey(GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        if(columnKey.isEmpty())
            return rowKey;
        if(rowKey.isEmpty())
            return columnKey;

        return GGroupObjectValue.checkTwins(new GGroupObjectValueBuilder()
                    .putAll(rowKey)
                    .putAll(columnKey).toGroupObjectValue());
    }

    private transient static NativeHashMap<GGroupObjectValue, GGroupObjectValue> twins = new NativeHashMap<>();

    public static GGroupObjectValue checkTwins(GGroupObjectValue value) {
        GGroupObjectValue twinValue = twins.get(value);
        if(twinValue == null) {
            twinValue = value;
            NativeHashMap<GGroupObjectValue, GGroupObjectValue> myTwins = GGroupObjectValue.twins;
            myTwins.put(value, value);

            if(GGroupObjectValue.twins.size() > 10000)
                GGroupObjectValue.twins = new NativeHashMap<>();
        }
        return twinValue;
    }

    public static ArrayList<GGroupObjectValue> checkTwins(ArrayList<GGroupObjectValue> values) {
        ArrayList<GGroupObjectValue> checked = null;
        for (int i = 0, valuesSize = values.size(); i < valuesSize; i++) {
            GGroupObjectValue value = values.get(i);
            GGroupObjectValue twinValue = checkTwins(value);
            if (checked == null) {
                if (twinValue != value) {
                    checked = new ArrayList<>();
                    for (int j = 0; j < i; j++)
                        checked.add(values.get(j));
                    checked.add(twinValue);
                }
            } else
                checked.add(twinValue);
        }

        return checked != null ? checked : values;
    }

    private void initSingle(Integer key, Object value) {
        size = 1;
        singleKey = key;
        singleValue = (Serializable) value;
    }

    public int getKey(int index) {
        return size == 1 ? singleKey : keys[index];
    }

    public Serializable getValue(int index) {
        return size == 1 ? singleValue : values[index];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GGroupObjectValue)) return false;

        GGroupObjectValue oth = (GGroupObjectValue) o;
        if (size != oth.size)
            return false;

        switch (size) {
            case 1:
                return singleKey == oth.singleKey && Objects.equals(singleValue, oth.singleValue);
            case 0:
                return true;
        }

        for (int i = 0; i < size; i++)
            if (keys[i] != oth.keys[i])
                return false;

        for (int i=0; i < size; i++)
            if (!Objects.equals(values[i], oth.values[i]))
                return false;

        return true;
    }

    transient private int hash;
    transient private boolean hashComputed;
    @Override
    public int hashCode() {
        if (!hashComputed) {
            if (size == 0) {
                hash = 0;
            } else if (size == 1) {
                hash = 31 * (31 + singleKey) + (singleValue == null ? 0 : singleValue.hashCode());
            } else {
                hash = size;
                hash = 31 * hash;
                int colHash = 1;
                for (int element : keys)
                    colHash = 31 * colHash + element;
                hash += colHash;
                hash = 31 * hash;
                colHash = 1;
                for (Serializable element : values)
                    colHash = 31 * colHash + (element == null ? 0 : element.hashCode());
                hash += colHash;
            }
            hashComputed = true;
        }
        return hash;
    }

    private transient String toString;
    // ===== the PUBLIC row-key contract, shared by CUSTOM REACT and the classic CUSTOM views =====
    // one public `key`: a JS PRIMITIVE wherever identity works out of the box (===, Map keys, property-name
    // coercion, React key=), otherwise the canonical injective string. NULL is 'n', not JS null (React key= /
    // byKey coercion footguns).
    public static final String KEY = "key"; // the public row-key field name (DISPLAY / React-key / diff-equality token; resolution uses the `objects` handle, not this)
    public static void setKey(JavaScriptObject row, GGroupObjectValue key) {
        Object single = key.size() == 1 ? key.getValue(0) : null;
        if (single instanceof GCustomObjectValue)
            setKeyNum(row, KEY, ((GCustomObjectValue) single).id); // ids are sequence-generated, nowhere near the 2^53 JS precision bound
        else if (single instanceof Number) // a numeric (non-object) key value is a native JS number too
            setKeyNum(row, KEY, ((Number) single).doubleValue());
        else
            setKeyStr(row, KEY, key.toKeyString());
    }
    private static native void setKeyNum(JavaScriptObject obj, String field, double v) /*-{ obj[field] = v; }-*/;
    private static native void setKeyStr(JavaScriptObject obj, String field, String v) /*-{ obj[field] = v; }-*/;

    // ===== the canonical string (one-way) =====
    // ENCODE computes: toKeyString() == String(row.key) — single: digits / the string itself / 'n'; multi: parts
    // joined with '|', each self-delimiting left-to-right (digits, 'n', or "len:value"), so distinct keys can't
    // produce one string. There is NO decode (the string omits the object-instance identity): it is a DISPLAY /
    // React-key / diff-equality token only, never a resolution input — resolution uses the row handle or a raw GGV.
    private transient String keyString; // hot: computed per row per list rebuild + diff equality
    public String toKeyString() {
        if (keyString != null)
            return keyString;
        return keyString = buildKeyString();
    }
    private String buildKeyString() {
        if (size == 1) {
            Object value = getValue(0);
            if (value instanceof GCustomObjectValue)
                return String.valueOf(((GCustomObjectValue) value).id); // == String(row.key) (a long id prints the same digits as the JS number)
            if (value instanceof Number)
                return jsNumberString(((Number) value).doubleValue()); // == String(key) of the JS number
            return value == null ? "n" : String.valueOf(value);
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) b.append('|');
            Object value = getValue(i);
            if (value instanceof GCustomObjectValue)
                b.append(((GCustomObjectValue) value).id);
            else if (value == null)
                b.append('n');
            else {
                String str = value instanceof Number ? jsNumberString(((Number) value).doubleValue()) : String.valueOf(value);
                b.append(str.length()).append(':').append(str);
            }
        }
        return b.toString();
    }
    // the EXACT JS String(number) — the canonical string must match what a caller-passed native number coerces
    // to, incl. -0/exponent edge cases Java formatting would diverge on
    private static native String jsNumberString(double value) /*-{ return String(value); }-*/;

    // ===== row identity: a platform-built row JS object (react-projected AND classic CUSTOM) carries its internal
    // GGroupObjectValue in an ENUMERABLE `objects` field — the same public field name CustomCellRenderer already uses, so
    // every CUSTOM surface (group rows + cell renderer) resolves rows uniformly. It joins `key`/`isCurrent` as a reserved
    // row-field name: an app property/column SID `objects` would be overwritten here, exactly as one named `key`/`isCurrent`
    // already is. Enumerable so a spread {...row} clone keeps it (a clone resolves as a row, like the original) and so it
    // travels through JSON/Object.assign with the row. It is identity, not content, so the classic list diff EXCLUDES it
    // via getObjectsField()=="objects" (no false-change). Survives list rebuilds (travels WITH the row); plain re-assign on
    // react's row reuse. =====
    public static final String ROW_OBJECTS = "objects"; // the public handle field name (matches CustomCellRenderer; reserved alongside `key`/`isCurrent`)
    public static void setRowObjects(JavaScriptObject row, GGroupObjectValue handle) {
        setField(row, ROW_OBJECTS, handle);
    }
    private static native void setField(JavaScriptObject row, String field, GGroupObjectValue handle) /*-{ row[field] = handle; }-*/;
    private static JavaScriptObject getRowObjects(JavaScriptObject row) {
        return readField(row, ROW_OBJECTS);
    }
    public static void clearRowObjects(JavaScriptObject row) { // drop the handle a clone inherited from its template (enumerable → copied by Object.assign)
        deleteField(row, ROW_OBJECTS);
    }
    private static native void deleteField(JavaScriptObject row, String field) /*-{ delete row[field]; }-*/;
    private static native JavaScriptObject readField(JavaScriptObject row, String field) /*-{
        return (row !== null && typeof row === 'object') ? row[field] : null;
    }-*/;

    // ===== the open-world handle: a JS value whose IDENTITY already IS a platform GGroupObjectValue (async OBJECTS
    // suggestions, getObjects round-trip handles) — the only way to name an object NOT in the current list. A plain
    // row/value JS object is not a Java GGV instance, so this returns null for them. =====
    public static GGroupObjectValue fromHandle(JavaScriptObject handle) {
        Object object = asObject(handle);
        return object instanceof GGroupObjectValue ? (GGroupObjectValue) object : null;
    }
    private static native Object asObject(JavaScriptObject handle) /*-{ return handle; }-*/;

    // ===== the row-identity API, shared by every surface (react projection + classic CUSTOM views + the form-level
    // controller). register = stamp the public `key` + the row-carried `objects` handle; resolve = a raw GGV passed
    // directly, OR a platform-built row (its `objects` handle, which a spread {...row} clone preserves). A bare scalar
    // key does NOT resolve — pass the row object or the raw GGV. =====
    public static void registerRow(JavaScriptObject row, GGroupObjectValue key) {
        setKey(row, key);
        setRowObjects(row, key);
    }
    public static GGroupObjectValue resolveObject(JavaScriptObject objectOrRow) {
        GGroupObjectValue handle = fromHandle(objectOrRow); // a raw GGV passed directly (open-world)
        return handle != null ? handle : fromHandle(getRowObjects(objectOrRow)); // a platform-built row's (or its clone's) `objects` handle
    }

    @Override
    public String toString() {
        if(toString == null) {
            if (size == 0) {
                toString = "[]";
            } else if (size == 1) {
                toString = "[" + singleKey + " = " + singleValue + "]";
            } else {
                StringBuilder caption = new StringBuilder("[");
                for (int i = 0; i < size; ++i) {
                    if (caption.length() > 1) {
                        caption.append(",");
                    }

                    caption.append(keys[i]).append("=").append(values[i]);
                }

                caption.append("]");
                toString = caption.toString();
            }
        }
        return toString;
    }

    // returns null, if there is an object that does not have value
    public GGroupObjectValue filter(List<GGroupObject> groups) {
        if(groups.isEmpty())
            return GGroupObjectValue.EMPTY;

        if(size == 0)
            return null;

        if(size == 1) {
            List<GObject> singleGroup;
            if(groups.size() == 1 && (singleGroup = groups.get(0).objects).size() == 1 && singleGroup.get(0).ID == singleKey)
                return this;
            return null;
        }

        int filteredSize = 0;
        NativeStringMap<Boolean> objects = new NativeStringMap<>();
        for(GGroupObject group : groups) {
            filteredSize += group.objects.size();
            for(GObject object : group.objects)
                objects.put(String.valueOf(object.ID), true);
        }

        int f = 0;
        int[] filteredKeys = new int[filteredSize];
        Serializable[] filteredValues = new Serializable[filteredSize];
        // we need to preserver keys order
        for(int i = 0 ; i < size ; i++) {
            int key = keys[i];
            if (objects.containsKey(String.valueOf(key))) {
                filteredKeys[f] = key;
                filteredValues[f++] = values[i];
            }
        }

        if(f < filteredSize) // there are some objects missing
            return null;

        return new GGroupObjectValue(filteredSize, filteredKeys, filteredValues);
    }
}
