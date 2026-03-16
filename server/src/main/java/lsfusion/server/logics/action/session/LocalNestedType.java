package lsfusion.server.logics.action.session;

public enum LocalNestedType {
    ALL, MANAGESESSION, NOMANAGESESSION;

    public static final LocalNestedType DATACHANGED = null;
    public static final LocalNestedType REQUESTCANCELED = LocalNestedType.ALL;

    public static final LocalNestedType RETURN = null;

    public static final LocalNestedType INPUT = null;
    public static final LocalNestedType IMPORT = null;
    public static final LocalNestedType FOR = null;

    public static final LocalNestedType FORM = LocalNestedType.ALL;
}
