package lsfusion.server.data.expr.formula;

public interface FormulaJoinImpl extends FormulaImpl {
    
    boolean hasNotNull(); // true - если может возвращать null, при не null аргументах
}
