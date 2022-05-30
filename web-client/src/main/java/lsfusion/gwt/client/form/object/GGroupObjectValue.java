package lsfusion.gwt.client.form.object;

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

    @Override
    public String toString() {
        if (size == 0) {
            return "[]";
        } else if (size == 1) {
            return "[" + singleKey + " = "  + singleValue + "]";
        } else {
            String caption = "[";
            for (int i = 0; i < size; ++i) {
                if (caption.length() > 1) {
                    caption += ",";
                }

                caption += keys[i] + "=" + values[i];
            }

            caption += "]";
            return caption;
        }
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
