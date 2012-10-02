package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.QuickMap;
import platform.base.TwinImmutableInterface;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcatenateClassSet;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.pull.ExprPullWheres;
import platform.server.data.query.stat.CalculateJoin;
import platform.server.data.query.stat.FormulaJoin;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.MapWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.ConcatenateType;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConcatenateExpr extends BaseExpr {

    private final List<BaseExpr> exprs;

    public BaseExpr deconcatenate(int part) {
        return exprs.get(part);
    }

    public ConcatenateExpr(List<BaseExpr> exprs) {
        this.exprs = exprs;
    }

    public static Expr create(List<? extends Expr> exprs) {
        return new ExprPullWheres<Integer>() {
            @Override
            protected Expr proceedBase(Map<Integer, BaseExpr> map) {
                return BaseExpr.create(new ConcatenateExpr(BaseUtils.toList(map)));
            }
        }.proceed(BaseUtils.toMap(exprs));
    }

    protected ConcatenateExpr translate(MapTranslate translator) {
        return new ConcatenateExpr(translator.translateDirect(exprs));
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(BaseExpr param : exprs)
            param.fillJoinWheres(joins, andWhere);
    }

    protected AndClassSet getPartClass(AndClassSet classes, int i) {
        return ((ConcatenateClassSet)classes).get(i);
    }
    protected AndClassSet getConcatenateClass(AndClassSet[] classes) {
        return new ConcatenateClassSet(classes);
    }
    public ClassExprWhere getClassWhere(AndClassSet classes) {
        ClassExprWhere result = ClassExprWhere.TRUE;
        for(int i=0;i<exprs.size();i++)
            result = result.and(exprs.get(i).getClassWhere(getPartClass(classes, i)));
        return result;
    }

    public AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and) {
        AndClassSet[] andClasses = new AndClassSet[exprs.size()];
        for(int i=0;i<exprs.size();i++) {
            AndClassSet classSet = exprs.get(i).getAndClassSet(and);
            if(classSet==null)
                return null;
            andClasses[i] = classSet;
        }
        return getConcatenateClass(andClasses);
    }

    public boolean addAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and, AndClassSet add) {
        for(int i=0;i<exprs.size();i++)
            if(!(exprs.get(i).addAndClassSet(and,getPartClass(add, i))))
                return false;
        return true;
    }

    public Expr classExpr(BaseClass baseClass) {
        throw new RuntimeException("not supported");
    }

    public Where isClass(AndClassSet set) {
        Where where = Where.TRUE;
        for(int i=0;i<exprs.size();i++)
            where = where.and(exprs.get(i).isClass(getPartClass(set, i)));
        return where;
    }

    public Expr translateQuery(QueryTranslator translator) {
        return create(translator.translate(exprs));
    }

    public boolean twins(TwinImmutableInterface obj) {
        return exprs.equals(((ConcatenateExpr)obj).exprs);
    }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return hashOuter(exprs, hashContext);
    }

    public Type getType(KeyType keyType) {
        Type[] types = new Type[exprs.size()];
        for(int i=0;i<exprs.size();i++)
            types[i] = exprs.get(i).getType(keyType);
        return new ConcatenateType(types);
    }

    public Stat getTypeStat(KeyStat keyStat) {
        Stat result = Stat.ONE;
        for (BaseExpr expr : exprs) result = result.mult(expr.getTypeStat(keyStat));
        return result;
    }

    public String getSource(CompileSource compile) {
        List<String> sources = new ArrayList<String>();
        for(BaseExpr expr : exprs)
            sources.add(expr.getSource(compile));
        return ((ConcatenateType)getType(compile.keyType)).getConcatenateSource(sources,compile.syntax);
    }

    @Override
    public Stat getStatValue(KeyStat keyStat) {
        return FormulaExpr.getStatValue(this, keyStat);
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return new FormulaJoin<Integer>(BaseUtils.toMap(exprs));
    }
}
