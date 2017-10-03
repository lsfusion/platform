package lsfusion.server.data.expr.formula;

public interface FormulaUnionImpl extends FormulaImpl {

    boolean supportRemoveNull(); // F(a,null,b)= F(a,b)

//    boolean supportEmptySimplify(); // F()= null, его FormulaExpr.create убирает

    boolean supportSingleSimplify(); // F(a) = a

    boolean supportNeedValue(); // при F(a,b) IS NOT NULL, одинаково для всех NOT NULL значений a и b 
}
