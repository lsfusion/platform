package lsfusion.server.data.expr.where.extra;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.interop.Compare;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.ParamLazy;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NotNullExpr;
import lsfusion.server.data.expr.NotNullExprInterface;
import lsfusion.server.data.query.*;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

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

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.<OuterContext>toSet(operator1, operator2);
    }

    public void fillDataJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        operator1.fillJoinWheres(joins,andWhere);
        operator2.fillJoinWheres(joins,andWhere);
    }

    protected ImSet<NotNullExprInterface> getExprFollows() {
        return operator1.getExprFollows(true, NotNullExpr.FOLLOW, true).merge(operator2.getExprFollows(true, NotNullExpr.FOLLOW, true));
    }

    protected abstract This createThis(BaseExpr operator1, BaseExpr operator2);
    protected abstract Compare getCompare();

    protected Where translate(MapTranslate translator) {
        return createThis(operator1.translateOuter(translator),operator2.translateOuter(translator));
    }
    @ParamLazy
    public Where translateQuery(QueryTranslator translator) {
        return operator1.translateQuery(translator).compare(operator2.translateQuery(translator),getCompare());
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

    public static boolean needIndexedJoin(BaseExpr expr, ImOrderSet<Expr> orderTop, BaseExpr valueExpr, Result<Boolean> resultIsOrderTop) {
        if((valueExpr == null || valueExpr.isValue()) && expr.isIndexed()) {
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
            else {
                Type type = valueExpr.getSelfType();
                return type != null && type.useIndexedJoin();
            }
        }
        return false;
    }

    public WhereJoin groupJoinsWheres(ImOrderSet<Expr> orderTop) {
        assert !getCompare().equals(Compare.EQUALS); // перегружена реализация по идее
        Result<Boolean> isOrderTop = new Result<>();
        if(needIndexedJoin(operator2, orderTop, operator1, isOrderTop)) // для Like'ов тоже надо так как там может быть git индекс
            return new ExprIndexedJoin(operator2, getCompare().reverse(), operator1, false, isOrderTop.result);
        if(needIndexedJoin(operator1, orderTop, operator2, isOrderTop))
            return new ExprIndexedJoin(operator1, getCompare(), operator2, false, isOrderTop.result);
        return null;
    }
    public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
        WhereJoin exprJoin = groupJoinsWheres(orderTop);
        if(exprJoin!=null)
            return new GroupJoinsWheres(exprJoin, this, type);
        return getOperandWhere().groupJoinsWheres(keepStat, keyStat, orderTop, type).and(super.groupJoinsWheres(keepStat, keyStat, orderTop, type));
    }

    @IdentityLazy
    protected Where getOperandWhere() {
        return operator1.getBinaryNotNullWhere(false).and(operator2.getBinaryNotNullWhere(false));
    }

    public ClassExprWhere calculateClassWhere() {
        return getOperandWhere().getClassWhere();
    }

    public boolean calcTwins(TwinImmutableObject obj) {
        return operator1.equals(((BinaryWhere)obj).operator1) && operator2.equals(((BinaryWhere)obj).operator2);
    }
    
    public static final String adjustSelectivity = "<<adj_sel>>";
    
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
