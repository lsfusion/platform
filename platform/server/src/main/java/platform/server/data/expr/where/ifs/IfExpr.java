package platform.server.data.expr.where.ifs;

import platform.server.caches.CacheAspect;
import platform.server.caches.hash.HashMapValues;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.*;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.cases.ExprCaseList;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.where.AbstractWhere;
import platform.server.data.where.Where;
import platform.server.data.where.MapWhere;
import platform.server.data.type.Type;
import platform.server.data.type.ClassReader;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.MapTranslate;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.ExprEnumerator;
import platform.server.classes.BaseClass;
import platform.server.classes.DataClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.caches.hash.HashContext;
import platform.server.caches.ManualLazy;
import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;
import platform.interop.Compare;

import java.util.HashSet;
import java.util.Set;

public class IfExpr extends Expr {

    public Where ifWhere;
    public Expr trueExpr;
    public Expr falseExpr;

    public IfExpr(Where ifWhere, Expr trueExpr, Expr falseExpr) {
        this.ifWhere = ifWhere;
        this.trueExpr = trueExpr;
        this.falseExpr = falseExpr;

        assert assertOrder(ifWhere, trueExpr, falseExpr);
    }

    private static boolean assertOrder(Where where, Expr trueExpr, Expr falseExpr) {
        int trueDepth = trueExpr.getWhereDepth(); int falseDepth = falseExpr.getWhereDepth();
        return trueDepth > falseDepth || (trueDepth==falseDepth && !where.isNot());
    }

    private int whereDepth = -1;
    @ManualLazy
    public int getWhereDepth() {
        if(whereDepth<0)
            whereDepth = trueExpr.getWhereDepth();
        return whereDepth;
    }

    private static Expr createPack(Where where, Expr trueExpr, Expr falseExpr, Where falseWhere, boolean pack) {
        trueExpr = trueExpr.followFalse(falseWhere.or(where.not()), pack);
        falseExpr = falseExpr.followFalse(falseWhere.or(where), pack);

        if(BaseUtils.hashEquals(trueExpr, falseExpr))
            return trueExpr;

       // вообще должен был быть equals (и верхняя проверка не нужна), но будет слишком сложное условие
        where = where.followFalse(falseWhere.or(trueExpr.getWhere().not().and(falseExpr.getWhere().not())), pack);
        if(where.isTrue())
            return trueExpr;
        if(where.isFalse())
            return falseExpr;
        // вопрос гнать рекурсию или нет???

        return createMean(where, trueExpr, falseExpr, pack, false);
    }

    // проверяем на следствия как в Where, если B=>A и A.FF(B) упрощается меняем местами
    // приходится проверять в обе стороны так как лексикографику по следствиям невозможно построить
    private static Expr createMean(Where where, Expr trueExpr, Expr falseExpr, boolean pack, boolean falseChecked) {
        if(trueExpr instanceof IfExpr) {
            IfExpr trueIfExpr = ((IfExpr)trueExpr);
            if(trueIfExpr.ifWhere.means(where)) {
                Where.FollowChange change = new Where.FollowChange();
                Where meanWhere = where.followFalse(trueIfExpr.ifWhere, pack, change);
                if(change.type!= Where.FollowType.EQUALS)
                    return createMerge(trueIfExpr.ifWhere, trueIfExpr.trueExpr, createMean(meanWhere, trueIfExpr.falseExpr, falseExpr, pack, falseChecked));
            }
        }

        if(falseChecked) {
            if(trueExpr.getWhereDepth() >= falseExpr.getWhereDepth())
                return createMerge(where, trueExpr, falseExpr);
            else
                return createMerge(where.not(), falseExpr, trueExpr);
        } else
            return createMean(where.not(), falseExpr, trueExpr, pack, true);
    }

    private static Expr createMerge(Where where, Expr trueExpr, Expr falseExpr) {
        assert trueExpr.getWhereDepth() >= falseExpr.getWhereDepth();
        // нужно найти в if'е равенства
        if(trueExpr instanceof IfExpr) { // рекурсию гнать смысла нет, так как внутри уже целостная структура и повторений не будет
            IfExpr trueIfExpr = ((IfExpr)trueExpr);
            if(BaseUtils.hashEquals(trueIfExpr.falseExpr, falseExpr))
                return createFinalOrder(where.and(trueIfExpr.ifWhere), trueIfExpr.trueExpr, falseExpr);
            if(BaseUtils.hashEquals(trueIfExpr.trueExpr, falseExpr))
                return createFinalOrder(trueIfExpr.ifWhere.or(where.not()), falseExpr, trueIfExpr.falseExpr);
        }
        return createFinalOrder(where, trueExpr, falseExpr);
    }

    private static Expr createFinalOrder(Where where, Expr trueExpr, Expr falseExpr) {
        if(trueExpr.getWhereDepth()==falseExpr.getWhereDepth() && where.isNot())
            return new IfExpr(where.not(), falseExpr, trueExpr);
        else
            return new IfExpr(where, trueExpr, falseExpr);
    }

    public static Expr create(Where where, Expr trueExpr, Expr falseExpr) {
        return createPack(where, trueExpr, falseExpr, Where.FALSE, false);
    }

    public Expr followFalse(Where where, boolean pack) {
        return createPack(ifWhere, trueExpr, falseExpr, where, pack);
    }

    public Type getType(KeyType keyType) { // порядок высот
        Type trueType = trueExpr.getType(keyType);
        assert trueType!=null;
        if(!(trueType instanceof DataClass))
            return trueType;
        Type falseType = falseExpr.getType(keyType);
        if(falseType==null)
            return trueType;
        else
            return ((DataClass)trueType).getCompatible((DataClass)falseType);
    }
    public Stat getTypeStat(Where fullWhere) {
        return trueExpr.getTypeStat(fullWhere.and(ifWhere));
    }

    public boolean twins(TwinImmutableInterface o) { // порядок высот / общий
        return ifWhere.equals(((IfExpr)o).ifWhere) && trueExpr.equals(((IfExpr)o).trueExpr) && falseExpr.equals(((IfExpr)o).falseExpr);
    }

    public int hashOuter(HashContext hashContext) { // порядок высот / общий
        return 31 * (31 * ifWhere.hashOuter(hashContext) + trueExpr.hashOuter(hashContext)) + falseExpr.hashOuter(hashContext);
    }

    public Where getBaseWhere() {
        return ifWhere;
    }

    public ClassReader getReader(KeyType keyType) {
        return getType(keyType);
    }

    public Where calculateWhere() {
        return ifWhere.ifElse(trueExpr.getWhere(), falseExpr.getWhere());
    }

    public ExprCaseList getCases() {
        throw new RuntimeException("not supported");
    }

    public Expr classExpr(BaseClass baseClass) {
        return trueExpr.classExpr(baseClass).ifElse(ifWhere, falseExpr.classExpr(baseClass));
    }

    public Where isClass(AndClassSet set) {
        return ifWhere.ifElse(trueExpr.isClass(set), falseExpr.isClass(set));
    }

    public Where compareBase(BaseExpr expr, Compare compareBack) {
        return ifWhere.ifElse(trueExpr.compareBase(expr, compareBack), falseExpr.compareBase(expr, compareBack));
    }

    public Where compare(Expr expr, Compare compare) {
        return ifWhere.ifElse(trueExpr.compare(expr, compare), falseExpr.compare(expr, compare));
    }

    public Expr translateQuery(QueryTranslator translator) {
        return IfExpr.create(ifWhere.translateQuery(translator), trueExpr.translateQuery(translator), falseExpr.translateQuery(translator));
    }

    protected long calculateComplexity() {
        return ifWhere.getComplexity() + trueExpr.getComplexity() + falseExpr.getComplexity();
    }

    public Expr translateOuter(MapTranslate translator) {
        return new IfExpr(ifWhere.translateOuter(translator), trueExpr.translateOuter(translator), falseExpr.translateOuter(translator));
    }

    public String getSource(CompileSource compile) {
        if (compile instanceof ToString)
            return "IF(" + ifWhere.getSource(compile) + "," + trueExpr.getSource(compile) + "," + falseExpr.getSource(compile) + ")";

        return "CASE WHEN " + ifWhere.getSource(compile) + " THEN " + trueExpr.getSource(compile) + " ELSE " + falseExpr.getSource(compile) + " END";
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        ifWhere.fillJoinWheres(joins, andWhere);
        trueExpr.fillJoinWheres(joins, andWhere.and(ifWhere));
        falseExpr.fillJoinWheres(joins, andWhere.and(ifWhere.not()));
    }

    public void enumDepends(ExprEnumerator enumerator) {
        ifWhere.enumerate(enumerator);
        trueExpr.enumerate(enumerator);
        falseExpr.enumerate(enumerator);
    }

    public Set<BaseExpr> getBaseExprs() {
        return BaseUtils.mergeSet(trueExpr.getBaseExprs(), falseExpr.getBaseExprs());
    }
}
