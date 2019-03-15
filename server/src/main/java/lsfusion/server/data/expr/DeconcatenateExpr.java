package lsfusion.server.data.expr;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.query.stat.PropStat;
import lsfusion.server.data.expr.query.stat.Stat;
import lsfusion.server.data.expr.query.stat.StatType;
import lsfusion.server.data.expr.where.pull.ExprPullWheres;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.compile.FJData;
import lsfusion.server.data.expr.join.stat.FormulaJoin;
import lsfusion.server.data.expr.join.stat.InnerBaseJoin;
import lsfusion.server.data.expr.join.stat.KeyStat;
import lsfusion.server.data.translator.ExprTranslator;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.struct.ConcatenateClassSet;
import lsfusion.server.logics.classes.user.set.AndClassSet;

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

    public void fillAndJoinWheres(MMap<FJData, Where> joins, Where andWhere) {
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

    public Expr translate(ExprTranslator translator) {
        return create(expr.translateExpr(translator),part,baseClass);
    }

    public boolean calcTwins(TwinImmutableObject obj) {
        return expr.equals(((DeconcatenateExpr)obj).expr) && part == ((DeconcatenateExpr)obj).part && baseClass.equals(((DeconcatenateExpr)obj).baseClass);  
    }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
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

    public String getSource(CompileSource compile, boolean needValue) {
        return ((ConcatenateType) expr.getType(compile.keyType)).getDeconcatenateSource(expr.getSource(compile, needValue), part, compile.syntax, compile.env);
    }

    public PropStat getStatValue(KeyStat keyStat, StatType type) {
        return FormulaExpr.getStatValue(this, keyStat);
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return new FormulaJoin<>(MapFact.singleton(0, expr), true); // хотя наверное можно и false
    }
}
