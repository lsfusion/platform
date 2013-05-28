package platform.server.data.expr;

import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.hash.HashContext;
import platform.server.classes.DataClass;
import platform.server.data.expr.formula.ExprSource;
import platform.server.data.expr.formula.FormulaImpl;
import platform.server.data.expr.formula.ListExprSource;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;

public class FormulaUnionExpr extends UnionExpr {

    private final ImList<Expr> exprs;
    private final FormulaImpl formula;
    private final ExprSource exprSource;

    private FormulaUnionExpr(FormulaImpl formula, ImList<Expr> exprs) {
        this.formula = formula;
        this.exprs = exprs;
        this.exprSource = new ListExprSource(exprs);
    }

    public static Expr create(final FormulaImpl formula, final ImList<Expr> exprs) {
        return new FormulaUnionExpr(formula, exprs);
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
