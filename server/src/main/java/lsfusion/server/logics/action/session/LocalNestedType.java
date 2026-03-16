package lsfusion.server.logics.action.session;

public enum LocalNestedType {
    ALL, MANAGESESSION, NOMANAGESESSION;

    public static final LocalNestedType DATACHANGED = null;
    public static final LocalNestedType REQUESTCANCELED = LocalNestedType.ALL;

    // need to survive newsession and even apply if called inside APPLY
    public static final LocalNestedType RETURN = LocalNestedType.ALL;

    // all this props are immediately read after write (so we don't need to nest them)
    public static final LocalNestedType INPUT = null;
    public static final LocalNestedType IMPORT = null;
    public static final LocalNestedType FOR = null;

    // ACTIVE / VIEW / SELECT should survive session operations
    public static final LocalNestedType FORM = LocalNestedType.ALL;
}
