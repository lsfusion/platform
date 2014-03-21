package lsfusion.server.data.expr;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.formula.FormulaExprInterface;
import lsfusion.server.data.expr.formula.FormulaJoinImpl;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.stat.InnerBaseJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

public class FormulaNotNullExpr extends StaticClassNotNullExpr implements FormulaExprInterface {

    protected final ImList<BaseExpr> exprs;

    protected final FormulaJoinImpl formula;

    public FormulaJoinImpl getFormula() {
        return formula;
    }

    public ImList<BaseExpr> getFParams() {
        return exprs;
    }

    public boolean hasFNotNull() {
        return true;
    }

    protected ImCol<Expr> getParams() {
        return BaseUtils.immutableCast(exprs.getCol());
    }

    public FormulaNotNullExpr(ImList<BaseExpr> exprs, FormulaJoinImpl formula) {
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

    public ConcreteClass getStaticClass() {
        return FormulaExpr.getStaticClass(this);
    }

    public Type getType(final KeyType keyType) {
        return FormulaExpr.getType(this, keyType);
    }

    public Expr translateQuery(QueryTranslator translator) {
        return FormulaExpr.translateQuery(this, translator);
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
        return exprs.equals(((FormulaNotNullExpr) o).exprs) && formula.equals(((FormulaNotNullExpr) o).formula);
    }

    protected int hash(HashContext hashContext) {
        return 31 * hashOuter(exprs, hashContext) + formula.hashCode();
    }

    protected FormulaNotNullExpr translate(MapTranslate translator) {
        return new FormulaNotNullExpr(translator.translateDirect(exprs), formula);
    }

    @IdentityLazy
    public Where getCommonWhere() {
        return getWhere(getFParams());
    }

    public class NotNull extends NotNullExpr.NotNull {

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
            return getCommonWhere().groupJoinsWheres(keepStat, keyStat, orderTop, type).and(new GroupJoinsWheres(this, type));
        }

        public ClassExprWhere calculateClassWhere() {
            return getCommonWhere().getClassWhere();
        }
    }

    public Where calculateNotNullWhere() {
        return new NotNull();
    }
}
