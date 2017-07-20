package lsfusion.gwt.form.shared.view.changes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GGroupObjectValue implements Serializable {
    public static final GGroupObjectValue EMPTY = new GGroupObjectValue();

    public static final List<GGroupObjectValue> SINGLE_EMPTY_KEY_LIST = Arrays.asList(EMPTY);

    private int size = 0;
    private int keys[];
    private Serializable values[];

    private int singleKey;
    private Serializable singleValue;

    public GGroupObjectValue() {
    }

    public GGroupObjectValue(int keys[], Serializable values[]) {
        if (keys == null || values == null) {
            throw new IllegalStateException("keys and values must not be null");
        } else {
            if (keys.length != values.length) {
                throw new IllegalStateException("keys and values must have the same size");
            }
            if (keys.length != 0) {
                if (keys.length == 1) {
                    initSingle(keys[0], values[0]);
                } else {
                    size = keys.length;
                    this.keys = keys;
                    this.values = values;
                }
            }
        }
    }

    public GGroupObjectValue(Map<Integer, Object> k) {
        int ks = k.size();
        if (ks != 0) {
            if (ks == 1) {
                Map.Entry<Integer, Object> e = k.entrySet().iterator().next();
                initSingle(e.getKey(), e.getValue());
            } else {
                size = ks;
                keys = new int[size];
                values = new Serializable[size];

                int i = 0;
                for (Map.Entry<Integer, Object> e : k.entrySet()) {
                    keys[i] = e.getKey();
                    values[i++] = (Serializable) e.getValue();
                }
            }
        }
    }

    public GGroupObjectValue(int key, Object value) {
        initSingle(key, value);
    }

    private void initSingle(Integer key, Object value) {
        size = 1;
        singleKey = key;
        singleValue = (Serializable) value;
    }

    public int getKey(int index) {
        return size == 1 ? singleKey : keys[index];
    }

    public Object getValue(int index) {
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
        if (size != oth.size) {
            return false;
        }

        if (size == 1) {
            return singleKey == oth.singleKey &&
                    (singleValue == oth.singleValue
                            || (singleValue != null && singleValue.equals(oth.singleValue)));
        }

        return size == 0 || (Arrays.equals(keys, oth.keys) && Arrays.equals(values, oth.values));
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
                hash = 31 * hash + Arrays.hashCode(keys);
                hash = 31 * hash + Arrays.hashCode(values);
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
}
