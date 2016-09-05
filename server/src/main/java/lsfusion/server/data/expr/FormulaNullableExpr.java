package lsfusion.server.data.expr;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.formula.FormulaExprInterface;
import lsfusion.server.data.expr.formula.FormulaJoinImpl;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.stat.InnerBaseJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.ExprTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;

public class FormulaNullableExpr extends StaticClassNullableExpr implements FormulaExprInterface {

    protected final ImList<BaseExpr> exprs;

    protected final FormulaJoinImpl formula;

    public FormulaJoinImpl getFormula() {
        return formula;
    }

    public ImList<BaseExpr> getFParams() {
        return exprs;
    }

    protected ImCol<Expr> getParams() {
        return BaseUtils.immutableCast(exprs.getCol());
    }

    public FormulaNullableExpr(ImList<BaseExpr> exprs, FormulaJoinImpl formula) {
        assert formula.hasNotNull();
        this.exprs = exprs;
        this.formula = formula;
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        FormulaExpr.fillAndJoinWheres(this, joins, andWhere);
    }

    public Expr packFollowFalse(Where where) {
        return FormulaExpr.packFollowFalse(this, where);
    }

    public String getSource(final CompileSource compile) {
        return FormulaExpr.getSource(this, compile);
    }
    public String toString() {
        return FormulaExpr.toString(this);
    }

    public ConcreteClass getStaticClass() {
        return FormulaExpr.getStaticClass(this);
    }
    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        return FormulaExpr.getFormulaAndClassSet(this, and);
    }

    public Type getType(final KeyType keyType) {
        return FormulaExpr.getType(this, keyType);
    }

    public Expr translate(ExprTranslator translator) {
        return FormulaExpr.translateExpr(this, translator);
    }

    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return FormulaExpr.getTypeStat(this, keyStat, forJoin);
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return FormulaExpr.getBaseJoin(this);
    }

    protected boolean isComplex() {
        return FormulaExpr.isComplex(this);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return exprs.equals(((FormulaNullableExpr) o).exprs) && formula.equals(((FormulaNullableExpr) o).formula);
    }

    protected int hash(HashContext hashContext) {
        return 31 * hashOuter(exprs, hashContext) + formula.hashCode();
    }

    protected FormulaNullableExpr translate(MapTranslate translator) {
        return new FormulaNullableExpr(translator.translateDirect(exprs), formula);
    }
}
