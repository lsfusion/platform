package lsfusion.server.logics.action.session;

public enum LocalNestedType {
    ALL, MANAGESESSION, NOMANAGESESSION;

    public static final LocalNestedType DATACHANGED = null;

    // need to survive newsession and even apply if called inside APPLY
    public static final LocalNestedType RETURN = LocalNestedType.ALL;

    // all this props are immediately read after write (so we don't need to nest them)
    public static final LocalNestedType REQUESTED = LocalNestedType.ALL; // should correspond requested* properties, and they are nested apparently for the group change mechanism (which splits write and read)
    public static final LocalNestedType IMPORT = null;
    public static final LocalNestedType FOR = null;

    // ACTIVE / VIEW / SELECT should survive session operations
    public static final LocalNestedType FORM = LocalNestedType.ALL;
}
