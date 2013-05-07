package platform.server.data.expr;

import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.server.caches.hash.HashContext;
import platform.server.classes.DataClass;
import platform.server.data.expr.formula.CustomFormulaImpl;
import platform.server.data.expr.formula.ExprSource;
import platform.server.data.expr.formula.ListExprSource;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;

public class FormulaUnionExpr extends UnionExpr {

    private final ImList<Expr> exprs;
    private final CustomFormulaImpl formula;
    private final ExprSource exprSource;

    private FormulaUnionExpr(String formula, DataClass dataClass, ImMap<String, Integer> mapParams, ImList<Expr> exprs) {
        this(new CustomFormulaImpl(formula, mapParams, dataClass), exprs);
    }

    private FormulaUnionExpr(CustomFormulaImpl formula, ImList<Expr> exprs) {
        this.formula = formula;
        this.exprs = exprs;
        this.exprSource = new ListExprSource(exprs);
    }

    public static FormulaUnionExpr create(String formula, DataClass dataClass, ImMap<String, Expr> params) {
        ImOrderSet<String> keys = params.keys().toOrderSet();

        ImMap<String, Integer> mapParams = keys.mapOrderValues(new GetIndex<Integer>() {
            @Override
            public Integer getMapValue(int i) {
                return i;
            }
        });
        ImList<Expr> exprs = keys.map(params);

        return new FormulaUnionExpr(formula, dataClass, mapParams, exprs);
    }

    public DataClass getStaticClass() {
        return (DataClass) formula.getStaticClass(exprSource);
    }

    protected ImSet<Expr> getParams() {
        return exprs.toOrderSet().getSet();
    }

    protected VariableSingleClassExpr translate(MapTranslate translator) {
        return new FormulaUnionExpr(formula, translator.translate(exprs));
    }

    public Expr translateQuery(QueryTranslator translator) {
        return new FormulaUnionExpr(formula, translator.translate(exprs));
    }

    public String getSource(CompileSource compile) {
        return formula.getSource(compile, exprSource);
    }

    protected int hash(HashContext hashContext) {
        return 31*formula.hashCode() * 31 + hashOuter(exprs, hashContext);
    }

    public boolean twins(TwinImmutableObject o) {
        return formula.equals(((FormulaUnionExpr)o).formula) && exprs.equals(((FormulaUnionExpr)o).exprs);
    }
}
