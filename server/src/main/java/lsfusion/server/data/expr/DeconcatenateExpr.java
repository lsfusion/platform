package lsfusion.server.data.expr;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ConcatenateClassSet;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.where.pull.ExprPullWheres;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.stat.FormulaJoin;
import lsfusion.server.data.query.stat.InnerBaseJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

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

    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return expr.getTypeStat(keyStat, forJoin);
    }

    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        AndClassSet classSet = expr.getAndClassSet(and);
        if(classSet == null)
            return null;
        return ((ConcatenateClassSet) classSet).get(part);
    }

    public Expr translateQuery(QueryTranslator translator) {
        return create(expr.translateQuery(translator),part,baseClass);
    }

    public boolean calcTwins(TwinImmutableObject obj) {
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

    public boolean addAndClassSet(MMap<VariableSingleClassExpr, AndClassSet> and, AndClassSet add) {
        ConcatenateType type = (ConcatenateType) expr.getSelfType();
        ImList<AndClassSet> list = BaseUtils.single(type.getUniversal(baseClass, part, add));
        return expr.addAndClassSet(and, new ConcatenateClassSet(list.toArray(new AndClassSet[list.size()])));
    }

    public String getSource(CompileSource compile) {
        return ((ConcatenateType) expr.getType(compile.keyType)).getDeconcatenateSource(expr.getSource(compile), part, compile.syntax, compile.env);
    }

    public PropStat getStatValue(KeyStat keyStat) {
        return FormulaExpr.getStatValue(this, keyStat);
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return new FormulaJoin<Integer>(MapFact.singleton(0, expr));
    }
}
