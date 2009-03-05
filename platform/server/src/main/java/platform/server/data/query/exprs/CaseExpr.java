package platform.server.data.query.exprs;

import platform.server.data.query.*;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.Where;

import java.util.*;


public class CaseExpr extends SourceExpr implements CaseWhere<ExprCase> {

    ExprCaseList cases;

    // этот конструктор нужен для создания CaseExpr'а в результать mapCase'а
    CaseExpr(ExprCaseList iCases) {
        cases = iCases;
    }

    public CaseExpr(Where where,SourceExpr expr) {
        cases = new ExprCaseList(where,expr);
    }

    public CaseExpr(Where where,SourceExpr exprTrue,SourceExpr exprFalse) {
        cases = new ExprCaseList(where,exprTrue,exprFalse);
    }

/*    CaseExpr(SourceExpr Expr,SourceExpr OpExpr,boolean Sum) {
        Cases = new ExprCaseList(Expr,OpExpr,Sum);
    }*/

    // получает список ExprCase'ов
    ExprCaseList getCases() {
        return cases;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {

        String source = "CASE";
        boolean noElse = false;
        for(int i=0;i< cases.size();i++) {
            ExprCase exprCaseCase = cases.get(i);
            String caseSource = exprCaseCase.data.getSource(queryData, syntax);

            if(i== cases.size()-1 && exprCaseCase.where.isTrue()) {
                source = source + " ELSE " + caseSource;
                noElse = true;
            } else
                source = source + " WHEN " + exprCaseCase.where.getSource(queryData, syntax) + " THEN " + caseSource;
        }
        return source + (noElse?"":" ELSE "+ Type.NULL)+" END";
    }

    public String toString() {
        String result = "";
        for(ExprCase exprCase : cases)
            result = (result.length()==0?"":result+",")+exprCase.toString();
        return "CE(" + result + ")";
    }

    public Type getType() {
        return cases.get(0).data.getType();
    }

    // не means OR верхних +
    // нету NullExpr'ов в ExprCase'е +
    // нету одинаковых AndExpr'ов 
    // пустой ExprCase - null
    // из одного элемента который means Expr.getWhere - по сути достаточно чистого Expr'а (не обязательно)
    // вообще разделение на translate и compile такое что все что быстро выполняется лучше сюда, остальное в Compile

    // все без not'ов (means,andNot)
    public SourceExpr translate(Translator translator) {

        ExprCaseList translatedCases = new ExprCaseList();
        for(ExprCase exprCase : cases) {
            Where translatedWhere = exprCase.where.translate(translator);
            if(translator.direct())
                translatedCases.add(new ExprCase(translatedWhere,exprCase.data.translate(translator))); // здесь на самом деле заведомо будут AndExpr'ы
            else {
                for(ExprCase translatedCase : exprCase.data.translate(translator).getCases())
                    translatedCases.add(translatedWhere.and(translatedCase.where),translatedCase.data);
                translatedCases.add(translatedWhere,getType().getExpr(null));
            }
        }

        if(translator.direct())
            return new CaseExpr(translatedCases);
        else
            return translatedCases.getExpr(getType());
    }

    public SourceExpr followFalse(Where where) {
        if(where.isTrue()) return this;

        ExprCaseList followedCases = new ExprCaseList(where);
        for(ExprCase exprCase : cases)
            followedCases.add(exprCase.where,exprCase.data);
        return followedCases.getExpr(getType());
    }

    static private <K> void recTranslateCase(ListIterator<Map.Entry<K, ? extends SourceExpr>> ic, MapCase<K> current, MapCaseList<K> result, boolean elseCase) {

        if(!ic.hasNext()) {
            result.add(current.where,new HashMap<K, AndExpr>(current.data));
            return;
        }

        Map.Entry<K,? extends SourceExpr> mapExpr = ic.next();

        for(ExprCase exprCase : mapExpr.getValue().getCases()) {
            Where prevWhere = current.where;
            current.where = current.where.and(exprCase.where);
            current.data.put(mapExpr.getKey(), (AndExpr) exprCase.data);
            recTranslateCase(ic,current,result, elseCase);
            current.data.remove(mapExpr.getKey());
            current.where = prevWhere;
        }
        if(elseCase)
            recTranslateCase(ic,current,result,true);

        ic.previous();
    }

    // строит комбинации из ExprCase'ов
    public static <K> MapCaseList<K> translateCase(Map<K, ? extends SourceExpr> mapExprs, Translator translator, boolean forceTranslate, boolean elseCase) {
        Map<K, SourceExpr> translateExprs = new HashMap<K, SourceExpr>();
        boolean hasCases = false;
        for(Map.Entry<K,? extends SourceExpr> mapExpr : mapExprs.entrySet()) {
            SourceExpr translatedExpr = mapExpr.getValue().translate(translator);
            translateExprs.put(mapExpr.getKey(), translatedExpr);
            hasCases = hasCases || translatedExpr instanceof CaseExpr;
        }

        if(!forceTranslate && !hasCases && translateExprs.equals(mapExprs))
            return null;
        else {
            MapCaseList<K> result = new MapCaseList<K>();
            recTranslateCase(new ArrayList<Map.Entry<K,? extends SourceExpr>>(translateExprs.entrySet()).listIterator(),new MapCase<K>(),result, elseCase);
            return result;
        }
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        for(ExprCase Case : cases) {
            Case.where.fillJoins(joins, values);
            Case.data.fillJoins(joins, values);
        }
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        // здесь по-хорошему надо andNot(верхних) но будет тормозить
        for(ExprCase Case : cases) {
            Case.where.fillJoinWheres(joins, andWhere);
            Case.data.fillJoinWheres(joins, andWhere.and(Case.where));
        }
    }

    public Where getCaseWhere(ExprCase cCase) {
        return cCase.data.getWhere();
    }

    // возвращает Where без следствий
    Where calculateWhere() {
        return cases.getWhere(this);
    }

    public int hashCode() {
        return cases.hashCode();
    }

    public boolean equals(Object obj) {
        return this==obj || obj instanceof CaseExpr && cases.equals(((CaseExpr)obj).cases);
    }

    // для кэша
    public boolean equals(SourceExpr expr, Map<ValueExpr, ValueExpr> mapValues, Map<KeyExpr, KeyExpr> mapKeys, MapJoinEquals mapJoins) {
        if(!(expr instanceof CaseExpr)) return false;

        CaseExpr caseExpr = (CaseExpr) expr;

        if(cases.size()!=caseExpr.cases.size()) return false;

        for(int i=0;i< cases.size();i++)
            if(!cases.get(i).equals(caseExpr.cases.get(i), mapValues, mapKeys, mapJoins))
                return false;

        return true;
    }

    int getHash() {
        int hash = 0;
        for(ExprCase exprCase : cases)
            hash = 31*hash + exprCase.hash();
        return hash;
    }
}
