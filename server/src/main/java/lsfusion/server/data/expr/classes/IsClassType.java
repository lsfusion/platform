package lsfusion.server.data.expr.classes;

public enum IsClassType {
    CONSISTENT, INCONSISTENT, SUMCONSISTENT, AGGCONSISTENT;

    public boolean isInconsistent() {
        return this == INCONSISTENT || this == SUMCONSISTENT || this == AGGCONSISTENT;
    }
}
