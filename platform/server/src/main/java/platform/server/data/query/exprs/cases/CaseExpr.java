package platform.server.data.query.exprs.cases;

import platform.base.BaseUtils;
import platform.server.data.classes.BaseClass;
import platform.server.data.classes.where.ClassSet;
import platform.server.data.query.*;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.translators.Translator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.data.types.ObjectType;
import platform.server.data.types.Reader;
import platform.server.data.types.NullReader;
import platform.server.where.Where;

import java.util.*;


public class CaseExpr extends SourceExpr {

    private final ExprCaseList cases;

    public CaseExpr() {
        this(new ExprCaseList());
    }

    // этот конструктор нужен для создания CaseExpr'а в результать mapCase'а
    public CaseExpr(ExprCaseList iCases) {
        cases = iCases;
        if(cases.size()>0) {
            ExprCase lastCase = cases.get(cases.size()-1); // в последнем элементе срезаем null'ы с конца
            lastCase.where = lastCase.where.followFalse(lastCase.data.getWhere().not());
        }
    }

    public CaseExpr(Where where,SourceExpr expr) {
        this(new ExprCaseList(where,expr));
    }

    public CaseExpr(Where where, SourceExpr exprTrue, SourceExpr exprFalse) {
        this(new ExprCaseList(where,exprTrue,exprFalse));
    }

    // получает список ExprCase'ов
    public ExprCaseList getCases() {
        return cases;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {

        if(cases.size()==0) return SQLSyntax.NULL;
        if(cases.size()==1 && cases.get(0).where.isTrue()) return cases.get(0).data.getSource(queryData, syntax);

        String source = "CASE";
        boolean noElse = false;
        for(int i=0;i<cases.size();i++) {
            ExprCase exprCaseCase = cases.get(i);
            String caseSource = exprCaseCase.data.getSource(queryData, syntax);

            if(i== cases.size()-1 && exprCaseCase.where.isTrue()) {
                source = source + " ELSE " + caseSource;
                noElse = true;
            } else
                source = source + " WHEN " + exprCaseCase.where.getSource(queryData, syntax) + " THEN " + caseSource;
        }
        return source + (noElse?"":" ELSE "+ SQLSyntax.NULL)+" END";
    }

    public String toString() {
        String result = "";
        for(ExprCase exprCase : cases)
            result = (result.length()==0?"":result+",")+exprCase.toString();
        return "CE(" + result + ")";
    }

    public Type getType(Where where) {
        assert !cases.isEmpty();
        return cases.iterator().next().data.getType(where);
    }

    public Reader getReader(Where where) {
        if(cases.isEmpty()) return NullReader.instance;
        return getType(where);
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
        for(ExprCase exprCase : cases)
            translatedCases.add(exprCase.where.translate(translator),exprCase.data.translate(translator));
        return new CaseExpr(translatedCases);
    }

    public SourceExpr followFalse(Where where) {
        if(where.isFalse()) return this;

        ExprCaseList followedCases = new ExprCaseList(where); // where идет в up
        for(ExprCase exprCase : cases)
            followedCases.add(exprCase.where,exprCase.data);
        return new CaseExpr(followedCases);
    }

    static private <K> void recPullCases(ListIterator<Map.Entry<K, ? extends SourceExpr>> ic, MapCase<K> current, Where currentWhere, MapCaseList<K> result) {

        if(currentWhere.isFalse())
            return;

        if(!ic.hasNext()) {
            result.add(current.where,new HashMap<K, AndExpr>(current.data));
            return;
        }

        Map.Entry<K,? extends SourceExpr> mapExpr = ic.next();

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

    public static <K> MapCaseList<K> pullCases(Map<K, ? extends SourceExpr> mapExprs) {
        MapCaseList<K> result = new MapCaseList<K>();
        recPullCases(new ArrayList<Map.Entry<K,? extends SourceExpr>>(mapExprs.entrySet()).listIterator(),new MapCase<K>(),Where.TRUE,result);
        return result;
    }

    public int fillContext(Context context, boolean compile) {
        int level = -1;
        for(ExprCase exprCase : cases) {
            level = BaseUtils.max(exprCase.where.fillContext(context, compile),level);
            level = BaseUtils.max(exprCase.data.fillContext(context, compile),level);
        }
        return level;
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        // здесь по-хорошему надо andNot(верхних) но будет тормозить
        for(ExprCase exprCase : cases) {
            exprCase.where.fillJoinWheres(joins, andWhere);
            exprCase.data.fillJoinWheres(joins, andWhere.and(exprCase.where));
        }
    }

    protected int getHashCode() {
        return cases.hashCode();
    }

    public boolean equals(Object obj) {
        return this==obj || obj instanceof CaseExpr && cases.equals(((CaseExpr)obj).cases);
    }

    // для кэша
    public boolean equals(SourceExpr expr, MapContext mapContext) {
        return expr instanceof CaseExpr && cases.equals(((CaseExpr) expr).cases, mapContext);
    }

    protected int getHash() {
        return cases.hash();
    }

    // получение Where'ов

    protected Where calculateWhere() {
        return cases.getWhere(new CaseWhereInterface<AndExpr>(){
            public Where getWhere(AndExpr cCase) {
                return cCase.getWhere();
            }
        });
    }

    public Where getIsClassWhere(final ClassSet set) {
        return cases.getWhere(new CaseWhereInterface<AndExpr>(){
            public Where getWhere(AndExpr cCase) {
                return cCase.getIsClassWhere(set);
            }
        });
    }

    public Where compare(final SourceExpr expr, final int compare) {
        return cases.getWhere(new CaseWhereInterface<AndExpr>(){
            public Where getWhere(AndExpr cCase) {
                return cCase.compare(expr,compare);
            }
        });
    }

    // получение выражений

    public SourceExpr scale(int coeff) {
        if(coeff==1) return this;
        
        ExprCaseList result = new ExprCaseList();
        for(ExprCase exprCase : cases)
            result.add(new ExprCase(exprCase.where,exprCase.data.scale(coeff)));
        return new CaseExpr(result);
    }

    public SourceExpr getClassExpr(BaseClass baseClass) {
        ExprCaseList result = new ExprCaseList();
        for(ExprCase exprCase : cases)
            result.add(exprCase.where,exprCase.data.getClassExpr(baseClass));
        return new CaseExpr(result);
    }

    public SourceExpr sum(SourceExpr expr) {
        ExprCaseList result = new ExprCaseList();
        for(ExprCase exprCase : cases)
            result.add(exprCase.where,exprCase.data.sum(expr));
        result.add(Where.TRUE,expr); // если null то expr 
        return new CaseExpr(result);
    }
}
