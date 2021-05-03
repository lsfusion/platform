package lsfusion.server.logics.action.session.change;

public class CalcDataType {

    public static final CalcDataType EXPR = new CalcDataType();

    // used for Constraint Checked Property, need to exclude class checks, to avoid having unnecessary changes in asyncPropertyChanges when objects are added / removed
    public static final CalcDataType PULLEXPR = new CalcDataType();
}
