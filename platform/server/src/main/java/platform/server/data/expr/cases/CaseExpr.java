package platform.server.data.expr.cases;

import platform.interop.Compare;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.caches.Lazy;
import platform.server.classes.BaseClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.query.*;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.translator.DirectTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.TranslateExprLazy;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.NullReader;
import platform.server.data.type.Reader;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

import net.jcip.annotations.Immutable;

@TranslateExprLazy
@Immutable
public class CaseExpr extends Expr {

    private final ExprCaseList cases;

    // этот конструктор нужен для создания CaseExpr'а в результать mapCase'а
    public CaseExpr(ExprCaseList iCases) {
        cases = iCases;
        assert !(cases.size()==1 && cases.get(0).where.isTrue());
    }

    // получает список ExprCase'ов
    public ExprCaseList getCases() {
        return cases;
    }

    public String getSource(CompileSource compile) {

        if(compile instanceof ToString) {
            String result = "";
            for(ExprCase exprCase : cases)
                result = (result.length()==0?"":result+",")+exprCase.toString();
            return "CE(" + result + ")";
        }

        if(cases.size()==0) return SQLSyntax.NULL;

        String source = "CASE";
        boolean noElse = false;
        for(int i=0;i<cases.size();i++) {
            ExprCase exprCase = cases.get(i);
            String caseSource = exprCase.data.getSource(compile);

            if(i== cases.size()-1 && exprCase.where.isTrue()) {
                source = source + " ELSE " + caseSource;
                noElse = true;
            } else
                source = source + " WHEN " + exprCase.where.getSource(compile) + " THEN " + caseSource;
        }
        return source + (noElse?"":" ELSE "+ SQLSyntax.NULL)+" END";
    }

    public Type getType(Where where) {
        assert !cases.isEmpty();
        return cases.iterator().next().data.getType(where);
    }

    public Reader getReader(Where where) {
        if(cases.isEmpty()) return NullReader.instance;
        return getType(where);
    }

    @ParamLazy
    public CaseExpr translateDirect(DirectTranslator translator) {
        ExprCaseList translatedCases = new ExprCaseList();
        for(ExprCase exprCase : cases)
            translatedCases.add(new ExprCase(exprCase.where.translateDirect(translator),exprCase.data.translateDirect(translator)));
        return new CaseExpr(translatedCases);        
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        ExprCaseList translatedCases = new ExprCaseList();
        for(ExprCase exprCase : cases)
            translatedCases.add(exprCase.where.translateQuery(translator),exprCase.data.translateQuery(translator));
        return translatedCases.getExpr();
    }

    public Expr followFalse(Where where) {
        if(where.isFalse()) return this;

        ExprCaseList followedCases = new ExprCaseList(where); // where идет в up
        for(ExprCase exprCase : cases)
            followedCases.add(exprCase.where,exprCase.data);
        return followedCases.getExpr();
    }

    static private <K> void recPullCases(ListIterator<Map.Entry<K, ? extends Expr>> ic, MapCase<K> current, Where currentWhere, MapCaseList<K> result) {

        if(currentWhere.isFalse())
            return;

        if(!ic.hasNext()) {
            result.add(current.where,new HashMap<K, BaseExpr>(current.data));
            return;
        }

        Map.Entry<K,? extends Expr> mapExpr = ic.next();

        for(ExprCase exprCase : mapExpr.getValue().getCases()) {
            Where prevWhere = current.where;
            current.where = current.where.and(exprCase.where);
            current.data.put(mapExpr.getKey(),exprCase.data);
            recPullCases(ic,current,currentWhere.and(exprCase.data.getWhere()),result);
            current.data.remove(mapExpr.getKey());
            current.where = prevWhere;
        }

        ic.previous();
    }

    public static <K> MapCaseList<K> pullCases(Map<K, ? extends Expr> mapExprs) {
        MapCaseList<K> result = new MapCaseList<K>();
        recPullCases(new ArrayList<Map.Entry<K,? extends Expr>>(mapExprs.entrySet()).listIterator(),new MapCase<K>(),Where.TRUE,result);
        return result;
    }

    public void enumerate(SourceEnumerator enumerator) {
        for(ExprCase exprCase : cases) {
            exprCase.where.enumerate(enumerator);
            exprCase.data.enumerate(enumerator);
        }
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        // здесь по-хорошему надо andNot(верхних) но будет тормозить
        for(ExprCase exprCase : cases) {
            exprCase.where.fillJoinWheres(joins, andWhere);
            exprCase.data.fillJoinWheres(joins, andWhere.and(exprCase.where));
        }
    }

    public boolean twins(AbstractSourceJoin obj) {
        return cases.equals(((CaseExpr)obj).cases);
    }

    @Lazy
    public int hashContext(HashContext hashContext) {
        return cases.hashContext(hashContext);
    }

    // получение Where'ов

    public Where calculateWhere() {
        return cases.getWhere(new CaseWhereInterface<BaseExpr>(){
            public Where getWhere(BaseExpr cCase) {
                return cCase.getWhere();
            }
        });
    }

    public Where isClass(final AndClassSet set) {
        return cases.getWhere(new CaseWhereInterface<BaseExpr>(){
            public Where getWhere(BaseExpr cCase) {
                return cCase.isClass(set);
            }
        });
    }

    public Where compare(final Expr expr, final Compare compare) {
        return cases.getWhere(new CaseWhereInterface<BaseExpr>(){
            public Where getWhere(BaseExpr cCase) {
                return cCase.compare(expr,compare);
            }
        });
    }

    // получение выражений

    public Expr scale(int coeff) {
        if(coeff==1) return this;
        
        ExprCaseList result = new ExprCaseList();
        for(ExprCase exprCase : cases)
            result.add(new ExprCase(exprCase.where,exprCase.data.scale(coeff)));
        return result.getExpr();
    }

    public Expr classExpr(BaseClass baseClass) {
        ExprCaseList result = new ExprCaseList();
        for(ExprCase exprCase : cases)
            result.add(exprCase.where,exprCase.data.classExpr(baseClass));
        return result.getExpr();
    }

    public Expr sum(Expr expr) {
        ExprCaseList result = new ExprCaseList();
        for(ExprCase exprCase : cases)
            result.add(exprCase.where,exprCase.data.sum(expr));
        result.add(Where.TRUE,expr); // если null то expr 
        return result.getExpr();
    }

}
