package lsfusion.server.data.expr;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.expr.formula.ExprSource;
import lsfusion.server.data.expr.formula.FormulaImpl;
import lsfusion.server.data.expr.formula.ListExprSource;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;

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
