package lsfusion.server.data.expr.where.classes.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.ParamLazy;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NullableExpr;
import lsfusion.server.data.expr.NullableExprInterface;
import lsfusion.server.data.expr.join.select.ExprIndexedJoin;
import lsfusion.server.data.expr.join.where.GroupJoinsWheres;
import lsfusion.server.data.expr.join.where.WhereJoin;
import lsfusion.server.data.expr.value.StaticExpr;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.KeyStat;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.classes.data.time.TimeSeriesClass;

public abstract class BinaryWhere<This extends BinaryWhere<This>> extends DataWhere {

    public final BaseExpr operator1;
    public final BaseExpr operator2;

    protected BinaryWhere(BaseExpr operator1, BaseExpr operator2) {
        this.operator1 = operator1;
        this.operator2 = operator2;
    }

    public static boolean checkEquals(BaseExpr operator1, BaseExpr operator2) {
        return BaseUtils.hashEquals(operator1, operator2);
    }

    public static boolean checkStaticNotEquals(BaseExpr operator1, BaseExpr operator2) {
        assert checkStaticClass(operator1, operator2);
        assert operator1 instanceof StaticExpr || operator1 instanceof StaticParamNullableExpr;
        assert operator2 instanceof StaticExpr || operator2 instanceof StaticParamNullableExpr;
        assert operator1.getClass() == operator2.getClass();

        // не равны, и если static, то типы у них одинаковые, потому как в противном случае СУБД будет считать, что равны, а сервер приложений нет
        return !checkEquals(operator1, operator2) && !(operator1 instanceof StaticExpr && operator2 instanceof StaticExpr && !BaseUtils.hashEquals(((StaticExpr) operator1).getType(), ((StaticExpr) operator2).getType()));
    }

    public static boolean checkStaticClass(BaseExpr operator1, BaseExpr operator2) {
        final int class1 = operator1.getStaticEqualClass(); // по аналогии с использованием в EqualMap
        final int class2 = operator2.getStaticEqualClass();
        return class1 >= 0 && class2 >= 0 && (class1 == class2 || class1 >= BaseExpr.STATICEQUALCLASSES || class2 >= BaseExpr.STATICEQUALCLASSES);
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.toSet(operator1, operator2);
    }

    public void fillDataJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
        operator1.fillJoinWheres(joins,andWhere);
        operator2.fillJoinWheres(joins,andWhere);
    }

    protected ImSet<NullableExprInterface> getExprFollows() {
        return operator1.getExprFollows(true, NullableExpr.FOLLOW, true).merge(operator2.getExprFollows(true, NullableExpr.FOLLOW, true));
    }

    protected abstract This createThis(BaseExpr operator1, BaseExpr operator2);
    protected abstract Compare getCompare();

    protected Where translate(MapTranslate translator) {
        return createThis(operator1.translateOuter(translator),operator2.translateOuter(translator));
    }
    @ParamLazy
    public Where translate(ExprTranslator translator) {
        return operator1.translateExpr(translator).compare(operator2.translateExpr(translator),getCompare());
    }

    @Override
    public Where packFollowFalse(Where falseWhere) {
        ImRevMap<Integer,BaseExpr> opMap = MapFact.toRevMap(0, operator1, 1, operator2);
        ImMap<Integer,Expr> packOpMap = BaseExpr.packPushFollowFalse(opMap, falseWhere);
        if(BaseUtils.hashEquals(opMap, packOpMap))
            return this;
        else
            return packOpMap.get(0).compare(packOpMap.get(1), getCompare());
    }

    public static boolean needIndexedJoin(BaseExpr expr, Compare compare, BaseExpr valueExpr, ImOrderSet<Expr> orderTop, Result<Boolean> resultIsOrderTop) {
        if((valueExpr == null || valueExpr.isValue()) && expr.isIndexed(compare)) { // this indexed check is not that good here (at least for "greater" compares), see Property.getSelectStat comment
            boolean isOrderTop = orderTop.contains(expr);
            if(resultIsOrderTop != null)
                resultIsOrderTop.set(isOrderTop);
            if(isOrderTop) {
                if (valueExpr == null)
                    return !expr.hasALotOfNulls();
                else
                    return true; // тут надо смотреть на то сколько distinct'ов, хотя может и не надо, разве что если их очень мало и для одного distinct значения в n раз больше записей чем в окне
            }
            // доступ по индексированному полю, нужно для поиска интервалов в WhereJoins.getStatKeys() и соответствующего уменьшения статистики
            if(valueExpr == null)
                return false;
            if(compare == Compare.CONTAINS || compare == Compare.MATCH)
                return true;
            return valueExpr.getSelfType() instanceof TimeSeriesClass;
        }
        return false;
    }

    public WhereJoin groupJoinsWheres(ImOrderSet<Expr> orderTop) {
        Compare compare = getCompare();
        assert !compare.equals(Compare.EQUALS); // перегружена реализация по идее
        Result<Boolean> isOrderTop = new Result<>();
        if(needIndexedJoin(operator2, compare.reverse(), operator1, orderTop, isOrderTop)) // для Like'ов тоже надо так как там может быть git индекс
            return new ExprIndexedJoin(operator2, compare.reverse(), operator1, isOrderTop.result);
        if(needIndexedJoin(operator1, compare, operator2, orderTop, isOrderTop))
            return new ExprIndexedJoin(operator1, compare, operator2, isOrderTop.result);
        return null;
    }
    public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, StatType statType, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
        WhereJoin exprJoin = groupJoinsWheres(orderTop);
        if(exprJoin!=null)
            return groupDataJoinsWheres(exprJoin, type);
        return getOperandWhere().groupJoinsWheres(keepStat, statType, keyStat, orderTop, type).and(super.groupJoinsWheres(keepStat, statType, keyStat, orderTop, type));
    }

    @IdentityLazy
    protected Where getOperandWhere() {
        return operator1.getBinaryNotNullWhere(false).and(operator2.getBinaryNotNullWhere(false));
    }

    protected ClassExprWhere getOperandClassWhere() {
        return getOperandWhere().getClassWhere(); // по сути и так BaseExpr.getNotNullClassWhere но для лучшего кэширования идем через ветку getOperandWhere
    }
    
    public ClassExprWhere calculateClassWhere() {
        return getOperandClassWhere();
    }

    public boolean calcTwins(TwinImmutableObject obj) {
        return operator1.equals(((BinaryWhere)obj).operator1) && operator2.equals(((BinaryWhere)obj).operator2);
    }
    
    public static final String adjustSelectivity = SQLSession.getParamName("adjSel");
    
    protected boolean adjustSelectivity(SQLSyntax syntax) {
        return false;
    }
    
    protected abstract String getCompareSource(CompileSource compile);
    public String getSource(CompileSource compile) {
        return fixSelectivity(compile, getBaseSource(compile));
    }

    protected String getBaseSource(CompileSource compile) {
        return operator1.getSource(compile) + getCompareSource(compile) + operator2.getSource(compile);
    }

    private String fixSelectivity(CompileSource compile, String result) {
        if(adjustSelectivity(compile.syntax))
            result = "(" + result + adjustSelectivity + ")";
        return result;
    }

    protected String getNotSource(CompileSource compile) {
        String op1Source = operator1.getSource(compile);
        String op2Source = operator2.getSource(compile);

        String result = "";
        if(!compile.means(operator1.getBinaryNotNullWhere(true)))
            result = op1Source + " IS NULL";
        if(!compile.means(operator2.getBinaryNotNullWhere(true)))
            result = (result.length()==0?"":result+" OR ") + op2Source + " IS NULL";
        String compare = "NOT " + getBaseSource(compile);
        if(result.length()==0)
            result = compare;
        else
            result = "(" + result + " OR " + compare + ")";
        return fixSelectivity(compile, result);
    }

    protected static Where create(BaseExpr operator1, BaseExpr operator2, BinaryWhere where) {
        return create(where).and(operator1.getOrWhere().and(operator2.getOrWhere()));
    }
}
