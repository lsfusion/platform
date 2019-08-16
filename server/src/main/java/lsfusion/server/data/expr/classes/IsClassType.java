package lsfusion.server.data.expr.classes;

// for classes checking / recalculating классов and determining data class for unknown object 
public enum IsClassType {
    CONSISTENT,
    VIRTUAL, // the same as noInnerFollows in joins
    INCONSISTENT, SUMCONSISTENT, AGGCONSISTENT;

    public boolean isInconsistent() {
        return this == INCONSISTENT || this == SUMCONSISTENT || this == AGGCONSISTENT;
    }
}
