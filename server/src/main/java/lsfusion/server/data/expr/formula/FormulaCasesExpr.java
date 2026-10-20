package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.UnionExpr;
import lsfusion.server.data.expr.classes.VariableSingleClassExpr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.data.DataClass;

// a formula of operands with cases (IF, CASE) as they are: FormulaExpr.create pulls the cases out into a formula for
// every combination of them, the sum of N IF's into 2^N formulas of all N operands, until the result would be more
// complex than limitPullCasesComplexity
public class FormulaCasesExpr extends UnionExpr {

    private final ImList<Expr> exprs;
    private final FormulaJoinImpl formula;

    FormulaCasesExpr(FormulaJoinImpl formula, ImList<Expr> exprs) {
        this.formula = formula;
        this.exprs = exprs;
    }

    // NULL when any of the operands is, as a FormulaExpr
    @Override
    public Where calculateOrWhere() {
        return Expr.getWhere(exprs);
    }

    // and can be NULL itself when they are not, as a division by zero
    @Override
    protected boolean hasUnionNotNull() {
        return formula.hasNotNull(exprs);
    }

    public ConcreteClass getStaticClass(KeyType keyType) {
        FormulaClass staticValueClass = FormulaExpr.getStaticValueClass(formula);
        if(staticValueClass != null)
            return staticValueClass;
        return (DataClass) formula.getType(ListExprType.create(keyType, exprs));
    }

    protected ImCol<Expr> getParams() {
        return exprs.getCol();
    }

    protected VariableSingleClassExpr translate(MapTranslate translator) {
        return new FormulaCasesExpr(formula, translator.translate(exprs));
    }

    public Expr translate(ExprTranslator translator) {
        return FormulaExpr.create(formula, translator.translate(exprs));
    }

    public Expr packFollowFalse(Where where) {
        return FormulaExpr.create(formula, exprs.mapListValues(value -> value.followFalse(where, true)));
    }

    public String getSource(final CompileSource compile, boolean needValue) {
        return formula.getSource(new ListExprSource(exprs, needValue || formula.hasNotNull(exprs)) {
            public CompileSource getCompileSource() {
                return compile;
            }});
    }

    public int hash(HashContext hashContext) {
        return 31 * hashOuter(exprs, hashContext) + formula.hashCode();
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return formula.equals(((FormulaCasesExpr) o).formula) && exprs.equals(((FormulaCasesExpr) o).exprs);
    }
}
