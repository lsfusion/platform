package lsfusion.server.logics.form.interactive.property;

public enum AsyncMode {
    VALUES, // getting distinct values
    OBJECTVALUES, // getting distinct values, assuming that they are pretty unique
    OBJECTS // getting objects with values (may be the same)
}
