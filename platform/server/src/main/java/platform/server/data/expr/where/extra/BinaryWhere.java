package platform.server.data.expr.where.extra;

import org.apache.xml.dtm.ref.DTMDefaultBaseIterators;
import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ParamLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.InnerExprSet;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import platform.server.data.where.MapWhere;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.ArrayList;
import java.util.Collection;

public abstract class BinaryWhere<This extends BinaryWhere<This>> extends DataWhere {

    public final BaseExpr operator1;
    public final BaseExpr operator2;

    protected BinaryWhere(BaseExpr operator1, BaseExpr operator2) {
        this.operator1 = operator1;
        this.operator2 = operator2;
    }

    public void enumDepends(ExprEnumerator enumerator) {
        operator1.enumerate(enumerator);
        operator2.enumerate(enumerator);
    }

    public void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        operator1.fillJoinWheres(joins,andWhere);
        operator2.fillJoinWheres(joins,andWhere);
    }

    private static Collection<BaseExpr> getAndOperands(BaseExpr operator1, BaseExpr operator2) {
        Collection<BaseExpr> follows = new ArrayList<BaseExpr>();
        if(!operator1.isOr())
            follows.add(operator1);
        if(!operator2.isOr())
            follows.add(operator2);
        return follows;
    }

    @IdentityLazy
    private Collection<BaseExpr> getAndOperands() {
        return getAndOperands(operator1, operator2);
    }

    public DataWhereSet calculateFollows() {
        return new DataWhereSet(new InnerExprSet(getAndOperands(), true));
    }

    protected abstract This createThis(BaseExpr operator1, BaseExpr operator2);
    protected abstract Compare getCompare();

    @ParamLazy
    public Where translateOuter(MapTranslate translator) {
        return createThis(operator1.translateOuter(translator),operator2.translateOuter(translator));
    }
    @ParamLazy
    public Where translateQuery(QueryTranslator translator) {
        return operator1.translateQuery(translator).compare(operator2.translateQuery(translator),getCompare());
    }

    @Override
    public Where packFollowFalse(Where falseWhere) {
        Expr packOperator1 = operator1.packFollowFalse(falseWhere);
        Expr packOperator2 = operator2.packFollowFalse(falseWhere);
        if(BaseUtils.hashEquals(packOperator1, operator1) && BaseUtils.hashEquals(packOperator2, operator2))
            return this;
        else
            return packOperator1.compare(packOperator2, getCompare());
    }

    public GroupJoinsWheres groupJoinsWheres() {
        return getOperandWhere().groupJoinsWheres().and(new GroupJoinsWheres(this));
    }

    protected Where getOperandWhere() {
        return Expr.getWhere(getAndOperands());
    }

    public long calculateComplexity() {
        return operator1.getComplexity() + operator2.getComplexity() + 1;
    }

    public ClassExprWhere calculateClassWhere() {
        return getOperandWhere().getClassWhere();
    }

    public boolean twins(TwinImmutableInterface obj) {
        return operator1.equals(((BinaryWhere)obj).operator1) && operator2.equals(((BinaryWhere)obj).operator2);
    }

    protected abstract String getCompareSource(CompileSource compile);
    public String getSource(CompileSource compile) {
        return operator1.getSource(compile) + getCompareSource(compile) + operator2.getSource(compile);
    }

    protected static Where create(BaseExpr operator1, BaseExpr operator2, BinaryWhere where) {
        return create(where).and(Expr.getWhere(getAndOperands(operator1, operator2)));
    }
}
