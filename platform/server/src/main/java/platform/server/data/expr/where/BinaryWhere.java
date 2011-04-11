package platform.server.data.expr.where;

import platform.base.TwinImmutableInterface;
import platform.interop.Compare;
import platform.server.caches.ParamLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.VariableExprSet;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.query.innerjoins.ObjectJoinSets;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.classes.LogicalClass;

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

    public DataWhereSet calculateFollows() {
        return new DataWhereSet(new VariableExprSet(operator1, operator2));
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
        return operator1.packFollowFalse(falseWhere).compare(operator2.packFollowFalse(falseWhere),getCompare());
    }

//    @Override
//    public KeyEquals calculateKeyEquals() {
//        return getOperandWhere().getKeyEquals().and(new KeyEquals(this));
//    }
    public ObjectJoinSets groupObjectJoinSets() {
        return getOperandWhere().groupObjectJoinSets().and(new ObjectJoinSets(this));
    }

    protected Where getOperandWhere() {
        return operator1.getWhere().and(operator2.getWhere());
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

}
