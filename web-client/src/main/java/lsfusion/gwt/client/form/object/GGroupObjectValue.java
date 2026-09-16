package lsfusion.gwt.client.form.object;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.base.GwtClientUtils;

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
    public static final String KEY = "key"; // the public row-key field name (DISPLAY / React-key / diff-equality token;
                                            // resolvable too, but only where the group to look it up in is known)
    // ===== the ONE place that decides what a key IS in JS, so the canonical string below is built from the same
    // decision - which makes `String(row.key) === toKeyString()` true by construction rather than by argument.
    // A feature that writes a key SOMEWHERE ELSE (a field pointing at another row, an element of an array) writes it
    // through here too, or `row.<field> === other.key` would not hold.
    public static void setKey(JavaScriptObject row, GGroupObjectValue key) {
        writeKey(row, KEY, key);
    }
    static void writeKey(JavaScriptObject target, String field, GGroupObjectValue key) {
        if (isNumberKey(key))
            writeKeyNum(target, field, numberKey(key));
        else
            writeKeyStr(target, field, key.toKeyString());
    }
    // a key of ONE object or ONE number is that number in JS; anything else is its canonical string. Asked and
    // answered as two calls rather than as one nullable Double: a key of 0 is a perfectly good key, and a boxed
    // number that has to be tested against null is the shape this codebase has been bitten by before
    private static boolean isNumberKey(GGroupObjectValue key) {
        Object single = key.size() == 1 ? key.getValue(0) : null;
        return single instanceof GCustomObjectValue || single instanceof Number;
    }
    // an id is sequence-generated, nowhere near the 2^53 bound a double stops being exact at
    private static double numberKey(GGroupObjectValue key) {
        Object single = key.getValue(0);
        return single instanceof GCustomObjectValue ? ((GCustomObjectValue) single).id : ((Number) single).doubleValue();
    }
    // field == null -> the target is an array and the value is pushed onto it
    private static native void writeKeyNum(JavaScriptObject t, String field, double v) /*-{ if (field == null) t.push(v); else t[field] = v; }-*/;
    private static native void writeKeyStr(JavaScriptObject t, String field, String v) /*-{ if (field == null) t.push(v); else t[field] = v; }-*/;

    // ===== the canonical string (one-way) =====
    // ENCODE computes: toKeyString() == String(row.key) — single: digits / the string itself / 'n'; multi: parts
    // joined with '|', each self-delimiting left-to-right (digits, 'n', or "len:value"). There is NO decode (the
    // string omits the object-instance identity), so nothing turns it back into a key: it resolves only by being
    // LOOKED UP among the rows of a known group - a row handle or a raw GGV is what resolves on its own.
    // The parts of a MULTI-value key cannot be misread for one another; a SINGLE value is written raw, so it is only
    // unambiguous against keys OF THE SAME SIZE - which is every row of one group, and hence every use that matters
    // (`byKey`, `keys`, the React key, row equality). Comparing across sizes is a tree's `parent` / `path`, and there
    // a single STRING-valued object key containing '|' could equal a composite of the same digits: an `OBJECTS s =
    // STRING` group above another group of a tree, with a '|' in the data. Raw stays raw because `row.key` being the
    // bare id is what every view reads; the alternative is length-prefixing every key to guard a case that needs a
    // string-keyed object, a tree, and a pipe in the value all at once.
    private transient String keyString; // hot: computed per row per list rebuild + diff equality
    public String toKeyString() {
        if (keyString != null)
            return keyString;
        return keyString = buildKeyString();
    }
    private String buildKeyString() {
        if (size == 1) {
            if (isNumberKey(this)) // the SAME decision the JS value is written by, so this is String() of it
                return jsNumberString(numberKey(this));
            Object value = getValue(0);
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
    // the identity of a key held OUTSIDE a group: the canonical string says what the key's values ARE, which tells two
    // rows apart within ONE group - every use that has a group (`byKey`, `keys`, the React key). A list an author
    // builds is scoped to nothing and can hold rows of several groups, and two of those can carry the same value; so
    // this names the objects the values are OF as well. Built ON the canonical string, so the two cannot part.
    public String toIdentityString() {
        StringBuilder b = new StringBuilder().append(size).append(':'); // how many ids follow, or an id could be read as part of the string
        for (int i = 0; i < size; i++)
            b.append(getKey(i)).append(':');
        return b.append(toKeyString()).toString();
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
    // a fabricated row: the template cloned, then keyed by whatever `objects` resolves to - the ONE place a custom
    // surface mints a row, so its public `key` and its `objects` handle cannot end up saying different things
    // (replacing the handle alone leaves the key it was cloned with, pointing at another row entirely)
    public static JavaScriptObject createRow(JavaScriptObject template, JavaScriptObject objects) {
        GGroupObjectValue key = resolveObject(objects); // a raw handle or a row of this form; anything else stays unkeyed
        JavaScriptObject created = GwtClientUtils.copyObject(template);
        if (key != null)
            registerRow(created, key);
        else
            clearRowIdentity(created); // the clone copied the template's key AND handle; a row that resolves to nothing
                                       // must not answer with the template's identity to a diff or to a lookup
        return created;
    }

    // both halves of a row's identity at once: the handle resolution reads, and the public key a diff and an index
    // compare by. Dropping one and keeping the other is what lets a fabricated row say it is its template
    private static void clearRowIdentity(JavaScriptObject row) {
        clearRowObjects(row);
        clearRowKey(row);
    }
    private static native void clearRowKey(JavaScriptObject row) /*-{ delete row[@lsfusion.gwt.client.form.object.GGroupObjectValue::KEY]; }-*/;

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
