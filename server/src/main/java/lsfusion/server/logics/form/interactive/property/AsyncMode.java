package lsfusion.server.logics.form.interactive.property;

public enum AsyncMode {
    VALUES, // getting distinct values
    STRICTVALUES, // getting distance values, but it's important to have the exact match
    OBJECTVALUES, // getting distinct values, assuming that they are pretty unique
    OBJECTS; // getting objects with values (may be the same)

    public boolean isValues() {
        return this == VALUES || this == STRICTVALUES;
    }

    public boolean isStrict() {
        return this != VALUES;
    }

    public boolean isObjects() {
        return this == OBJECTS;
    }

    public Object getCacheMode() {
        // OBJECTVALUES is transient (it is used only for optimization purposes, so we won't use it in cache)
        if(this == OBJECTVALUES)
            return VALUES;

        return this;
    }
}
