package platform.server.data.expr;

import platform.server.data.translator.DirectTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.query.JoinData;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.SourceEnumerator;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.ExprCase;
import platform.server.data.type.Type;
import platform.server.data.type.ConcatenateType;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcatenateClassSet;
import platform.server.caches.hash.HashContext;
import platform.server.caches.Lazy;
import platform.base.QuickMap;

import java.util.List;

import net.jcip.annotations.Immutable;

@Immutable
public class DeconcatenateExpr extends SingleClassExpr {

    BaseExpr expr;
    int part;

    BaseClass baseClass;

    public DeconcatenateExpr(BaseExpr expr, int part, BaseClass baseClass) {
        this.expr = expr;
        this.part = part;

        this.baseClass = baseClass;
    }

    public static Expr create(Expr expr, int part, BaseClass baseClass) {
        ExprCaseList result = new ExprCaseList();
        for(ExprCase exprCase : expr.getCases())
            result.add(exprCase.where, BaseExpr.create(new DeconcatenateExpr(exprCase.data, part, baseClass)));
        return result.getExpr();
    }


    public DeconcatenateExpr translateDirect(DirectTranslator translator) {
        return new DeconcatenateExpr(expr.translateDirect(translator), part, baseClass);
    }

    public VariableExprSet calculateExprFollows() {
        return expr.getExprFollows();
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        expr.fillJoinWheres(joins, andWhere);
    }

    public Type getType(Where where) {
        return ((ConcatenateType)expr.getType(where)).get(part);
    }
    
    public AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and) {
        return ((ConcatenateClassSet)expr.getAndClassSet(and)).get(part);
    }

    public Where calculateWhere() {
        return expr.getWhere();
    }

    public Expr translateQuery(QueryTranslator translator) {
        return create(expr.translateQuery(translator),part,baseClass);
    }

    public boolean twins(AbstractSourceJoin obj) {
        return expr.equals(((DeconcatenateExpr)obj).expr) && part == ((DeconcatenateExpr)obj).part && baseClass.equals(((DeconcatenateExpr)obj).baseClass);  
    }

    @Lazy
    public int hashContext(HashContext hashContext) {
        return expr.hashContext(hashContext) * 31 + part;
    }

    public void enumerate(SourceEnumerator enumerator) {
        expr.enumerate(enumerator);
    }

    public ClassExprWhere getClassWhere(AndClassSet classes) {
        ClassExprWhere result = ClassExprWhere.FALSE;

        ConcatenateType type = (ConcatenateType) expr.getSelfType();
        for(List<AndClassSet> list : type.getUniversal(baseClass,part,classes))
            result = result.or(expr.getClassWhere(new ConcatenateClassSet(list.toArray(new AndClassSet[1]))));

        return result;
    }

    public boolean addAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and, AndClassSet add) {
        throw new RuntimeException("not supported");
    }

    public String getSource(CompileSource compile) {
        return ((ConcatenateType) expr.getSelfType()).getDeconcatenateSource(expr.getSource(compile), part, compile.syntax);
    }
}
