package lsfusion.gwt.client.base;

import com.google.gwt.user.client.rpc.IsSerializable;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.PValue;

import java.io.Serializable;

public class GAsync implements IsSerializable, Serializable {
    public Serializable displayValue; // String or GStringWithFiles
    public Serializable rawValue;

    public Serializable key; // GGroupObjectValue or String

    public static final GAsync RECHECK = new GAsync("RECHECK", "RECHECK", null);
    public static final GAsync CANCELED = new GAsync("CANCELED", "CANCELED", null);

    public GAsync() {
    }

    public GAsync(Serializable displayValue, Serializable rawValue, Serializable key) {
        this.displayValue = displayValue;
        this.rawValue = rawValue;

        this.key = key;
    }

    public PValue getDisplayValue() {
        return PValue.remapValue(displayValue);
    }

    public PValue getRawValue() {
        return PValue.remapValue(rawValue);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GAsync && displayValue.equals(((GAsync) o).displayValue) && rawValue.equals(((GAsync) o).rawValue) && GwtClientUtils.nullEquals(key, ((GAsync) o).key);
    }

    @Override
    public int hashCode() {
        return 31 * (displayValue.hashCode() * 31 + rawValue.hashCode()) + GwtClientUtils.nullHash(key);
    }
}
