package lsfusion.server.logics.property;

public enum CalcType {
    EXPR, CLASS, STAT;
    
    public boolean isClass() {
        return this == CLASS;
    }

    public boolean isStat() {
        return this == STAT;
    }

    public boolean isExpr() {
        return this == EXPR; 
    }
}
