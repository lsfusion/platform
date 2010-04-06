package platform.server.data.expr;

import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.query.JoinData;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.SourceEnumerator;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.type.Type;
import platform.server.data.type.ConcatenateType;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcatenateClassSet;
import platform.server.classes.sets.AndClassSet;
import platform.server.caches.HashContext;
import platform.base.BaseUtils;
import platform.base.QuickMap;

import java.util.List;
import java.util.ArrayList;

public class ConcatenateExpr extends BaseExpr {

    private final List<BaseExpr> exprs;

    public ConcatenateExpr(List<BaseExpr> exprs) {
        this.exprs = exprs;
    }

    public static Expr create(List<? extends Expr> exprs) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<Integer> mapCase : CaseExpr.pullCases(BaseUtils.toMap(exprs)))
            result.add(mapCase.where, BaseExpr.create(new ConcatenateExpr(BaseUtils.toList(mapCase.data))));
        return result.getExpr();
    }

    public DataWhereSet getFollows() {
        return new DataWhereSet(exprs);
    }

    public ConcatenateExpr translateDirect(KeyTranslator translator) {
        return new ConcatenateExpr(translator.translateDirect(exprs));
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(BaseExpr param : exprs)
            param.fillJoinWheres(joins, andWhere);
    }

    public ClassExprWhere getClassWhere(AndClassSet classes) {
        ClassExprWhere result = ClassExprWhere.TRUE;
        for(int i=0;i<exprs.size();i++)
            result.and(exprs.get(i).getClassWhere(((ConcatenateClassSet)classes).get(i)));
        return result;
    }

    public AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and) {
        AndClassSet[] andClasses = new AndClassSet[exprs.size()];
        for(int i=0;i<exprs.size();i++)
            andClasses[i] = exprs.get(i).getAndClassSet(and);
        return new ConcatenateClassSet(andClasses);
    }

    public boolean addAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and, AndClassSet add) {
        for(int i=0;i<exprs.size();i++)
            if(!(exprs.get(i).addAndClassSet(and,((ConcatenateClassSet)add).get(i))))
                return false;
        return true;
    }

    public Where calculateWhere() {
        return getWhere(exprs);
    }

    public Expr classExpr(BaseClass baseClass) {
        throw new RuntimeException("not supported");
    }

    public Where isClass(AndClassSet set) {
        Where where = Where.TRUE;
        for(int i=0;i<exprs.size();i++)
            where = where.and(exprs.get(i).isClass(((ConcatenateClassSet)set).get(i)));
        return where;
    }

    public Expr translateQuery(QueryTranslator translator) {
        return create(translator.translate(exprs));
    }

    public boolean twins(AbstractSourceJoin obj) {
        return exprs.equals(((ConcatenateExpr)obj).exprs);
    }

    public int hashContext(HashContext hashContext) {
        int hash = 0;
        for(BaseExpr expr : exprs)
            hash = hash * 31 + expr.hashContext(hashContext);
        return hash;
    }

    public Type getType(Where where) {
        Type[] types = new Type[exprs.size()];
        for(int i=0;i<exprs.size();i++)
            types[i] = exprs.get(i).getType(where);
        return new ConcatenateType(types);
    }
    
    public String getSource(CompileSource compile) {
        List<String> sources = new ArrayList<String>();
        for(BaseExpr expr : exprs)
            sources.add(expr.getSource(compile));
        return ((ConcatenateType)getSelfType()).getConcatenateSource(sources,compile.syntax);
    }

    public void enumerate(SourceEnumerator enumerator) {
        enumerator.fill(exprs);
    }
}
