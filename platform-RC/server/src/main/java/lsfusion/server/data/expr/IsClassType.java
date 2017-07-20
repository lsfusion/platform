package lsfusion.server.data.expr;

public enum IsClassType {
    CONSISTENT, INCONSISTENT, SUMCONSISTENT, AGGCONSISTENT;

    public boolean isInconsistent() {
        return this == INCONSISTENT || this == SUMCONSISTENT || this == AGGCONSISTENT;
    }
}
