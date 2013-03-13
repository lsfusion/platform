package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcatenateClassSet;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.query.PropStat;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.pull.ExprPullWheres;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.stat.FormulaJoin;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.ConcatenateType;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

public class DeconcatenateExpr extends SingleClassExpr {

    BaseExpr expr;
    int part;

    BaseClass baseClass;

    public DeconcatenateExpr(BaseExpr expr, int part, BaseClass baseClass) {
        assert !(expr instanceof ConcatenateExpr);

        this.expr = expr;
        this.part = part;

        this.baseClass = baseClass;
    }

    private static Expr createBase(BaseExpr expr, int part, BaseClass baseClass) {
        if(expr instanceof ConcatenateExpr)
            return ((ConcatenateExpr)expr).deconcatenate(part);
        else
            return BaseExpr.create(new DeconcatenateExpr(expr, part, baseClass));
    }

    public static Expr create(Expr expr, final int part, final BaseClass baseClass) {
        return new ExprPullWheres<Integer>() {
            protected Expr proceedBase(ImMap<Integer, BaseExpr> map) {
                return createBase(map.get(0), part, baseClass);
            }
        }.proceed(MapFact.singleton(0, expr));
    }


    protected DeconcatenateExpr translate(MapTranslate translator) {
        return new DeconcatenateExpr(expr.translateOuter(translator), part, baseClass);
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        expr.fillJoinWheres(joins, andWhere);
    }

    public Type getType(KeyType keyType) {
        return ((ConcatenateType)expr.getType(keyType)).get(part);
    }

    public Stat getTypeStat(KeyStat keyStat) {
        return expr.getTypeStat(keyStat);
    }

    public AndClassSet getAndClassSet(ImMap<VariableClassExpr, AndClassSet> and) {
        AndClassSet classSet = expr.getAndClassSet(and);
        if(classSet == null)
            return null;
        return ((ConcatenateClassSet) classSet).get(part);
    }

    public Expr translateQuery(QueryTranslator translator) {
        return create(expr.translateQuery(translator),part,baseClass);
    }

    public boolean twins(TwinImmutableObject obj) {
        return expr.equals(((DeconcatenateExpr)obj).expr) && part == ((DeconcatenateExpr)obj).part && baseClass.equals(((DeconcatenateExpr)obj).baseClass);  
    }

    protected boolean isComplex() {
        return true;
    }
    protected int hash(HashContext hashContext) {
        return expr.hashOuter(hashContext) * 31 + part;
    }

    public ClassExprWhere getClassWhere(AndClassSet classes) {
        ClassExprWhere result = ClassExprWhere.FALSE;

        ConcatenateType type = (ConcatenateType) expr.getSelfType();
        for(ImList<AndClassSet> list : type.getUniversal(baseClass,part,classes))
            result = result.or(expr.getClassWhere(new ConcatenateClassSet(list.toArray(new AndClassSet[list.size()]))));

        return result;
    }

    public boolean addAndClassSet(MMap<VariableClassExpr, AndClassSet> and, AndClassSet add) {
        ConcatenateType type = (ConcatenateType) expr.getSelfType();
        ImList<AndClassSet> list = BaseUtils.single(type.getUniversal(baseClass, part, add));
        return expr.addAndClassSet(and, new ConcatenateClassSet(list.toArray(new AndClassSet[list.size()])));
    }

    public String getSource(CompileSource compile) {
        return ((ConcatenateType) expr.getType(compile.keyType)).getDeconcatenateSource(expr.getSource(compile), part, compile.syntax);
    }

    public PropStat getStatValue(KeyStat keyStat) {
        return FormulaExpr.getStatValue(this, keyStat);
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return new FormulaJoin<Integer>(MapFact.singleton(0, expr));
    }
}
