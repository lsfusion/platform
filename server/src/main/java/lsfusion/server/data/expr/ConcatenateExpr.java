package lsfusion.server.data.expr;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.ConcatenateClassSet;
import lsfusion.server.classes.ValueClassSet;
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
import lsfusion.server.logics.property.ClassField;

public class ConcatenateExpr extends VariableClassExpr {

    private final ImList<BaseExpr> exprs;

    public BaseExpr deconcatenate(int part) {
        return exprs.get(part);
    }

    public ConcatenateExpr(ImList<BaseExpr> exprs) {
        this.exprs = exprs;
    }

    public static Expr create(ImList<? extends Expr> exprs) {
        return new ExprPullWheres<Integer>() {
            @Override
            protected Expr proceedBase(ImMap<Integer, BaseExpr> map) {
                return BaseExpr.create(new ConcatenateExpr(ListFact.fromIndexedMap(map)));
            }
        }.proceed(exprs.toIndexedMap());
    }

    protected ConcatenateExpr translate(MapTranslate translator) {
        return new ConcatenateExpr(translator.translateDirect(exprs));
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
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

    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        AndClassSet[] andClasses = new AndClassSet[exprs.size()];
        for(int i=0;i<exprs.size();i++) {
            AndClassSet classSet = exprs.get(i).getAndClassSet(and);
            if(classSet==null)
                return null;
            andClasses[i] = classSet;
        }
        return getConcatenateClass(andClasses);
    }

    public boolean addAndClassSet(MMap<VariableSingleClassExpr, AndClassSet> and, AndClassSet add) {
        for(int i=0;i<exprs.size();i++)
            if(!(exprs.get(i).addAndClassSet(and,getPartClass(add, i))))
                return false;
        return true;
    }

    public Expr classExpr(ImSet<ClassField> classes, IsClassType type) {
        throw new RuntimeException("not supported");
    }

    public Where isClass(ValueClassSet set, boolean inconsistent) {
        Where where = Where.TRUE;
        for(int i=0;i<exprs.size();i++)
            where = where.and(exprs.get(i).isClass((ValueClassSet)getPartClass(set, i)), inconsistent);
        return where;
    }

    public Expr translateQuery(QueryTranslator translator) {
        return create(translator.translate(exprs));
    }

    public boolean twins(TwinImmutableObject obj) {
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
        return ConcatenateType.get(types);
    }

    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        Stat result = Stat.ONE;
        for (BaseExpr expr : exprs) result = result.mult(expr.getTypeStat(keyStat, forJoin));
        return result;
    }

    public String getSource(final CompileSource compile) {
        ImList<String> sources = exprs.mapListValues(new GetValue<String, BaseExpr>() {
            public String getMapValue(BaseExpr value) {
                return value.getSource(compile);
            }});
        return ((ConcatenateType)getType(compile.keyType)).getConcatenateSource(sources,compile.syntax,compile.env);
    }

    @Override
    public PropStat getStatValue(KeyStat keyStat) {
        return FormulaExpr.getStatValue(this, keyStat);
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return new FormulaJoin<Integer>(exprs.toIndexedMap());
    }
}
