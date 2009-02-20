package platformlocal;

import java.util.*;

abstract class Case<D> {
    Where where;
    D data;

    Case(Where iWhere,D iData) {
        where = iWhere;
        data = iData;
    }
}

interface CaseWhere<C> {
    Where getCaseWhere(C cCase);
}

abstract class CaseList<D,C extends Case<D>> extends ArrayList<C> {

    CaseList() {
    }
    CaseList(D data) {
        add(create(Where.TRUE,data));
    }
    CaseList(Where where,D data) {
        add(create(where, data));
        lastWhere = where;
    }
    CaseList(Where where,D dTrue,D dFalse) {
        super();
        add(create(where,dTrue));
        add(create(Where.TRUE,dFalse));
    }
    CaseList(Where falseWhere) {
        prevUpWhere = falseWhere;
    }

    Where prevUpWhere = Where.FALSE;
    Where lastWhere = null;
    Where getUpWhere() { // для оптимизации последних элементов
        if(lastWhere !=null) {
            prevUpWhere = prevUpWhere.or(lastWhere);
            lastWhere = null;
        }

        return prevUpWhere;
    }

    D followWhere(Where where, D data, Where upWhere) {
        return data;
    }

    // добавляет Case, проверяя все что можно
    void add(Where where,D data) {

        Where upWhere = getUpWhere();
        where = where.followFalse(upWhere);
        if(!where.isFalse()) {
            data = followWhere(where,data,upWhere);
            
            C lastCase = size()>0?get(size()-1):null;
            if(lastCase!=null && lastCase.data.equals(data)) // заOr'им
                lastCase.where = lastCase.where.or(where);
            else
                add(create(where, data));
            lastWhere = where;
        }
    }

    Where getWhere(CaseWhere<C> caseInterface) {

        Where result = Where.FALSE;
        Where up = Where.FALSE;
        for(C cCase : this) {
            Where CaseWhere = caseInterface.getCaseWhere(cCase);
            result = result.or(cCase.where.and(CaseWhere).and(up.not()));
            up = up.or(cCase.where);
        }

        return result;
    }
    
    abstract C create(Where Where,D Data);
}

class ExprCase extends Case<SourceExpr> {

    ExprCase(Where iWhere, AndExpr iExpr) {
        this(iWhere,(SourceExpr)iExpr);
    }

    // дублируем чтобы различать
    ExprCase(Where iWhere, SourceExpr iExpr) {
        super(iWhere,iExpr);
    }

    public String toString() {
        return where.toString() + "-" + data.toString();
    }

    public boolean equals(Object obj) {
        return this==obj || obj instanceof ExprCase && where.equals(((ExprCase)obj).where) && data.equals(((ExprCase)obj).data);
    }

    public int hashCode() {
        return where.hashCode()*31+data.hashCode();
    }

    // для кэша
    boolean equals(ExprCase Case, Map<ObjectExpr, ObjectExpr> MapExprs, Map<JoinWhere, JoinWhere> MapWheres) {
        return where.equals(Case.where, MapExprs, MapWheres) && data.equals(Case.data, MapExprs, MapWheres);
    }

    int hash() {
        return where.hash()*31+ data.hash();
    }
}

class ExprCaseList extends CaseList<SourceExpr,ExprCase> {

    ExprCaseList() {
    }
    ExprCaseList(SourceExpr data) {
        super(data);
    }
    ExprCaseList(Where where, SourceExpr data) {
        super(where, data);
    }
    ExprCaseList(Where where, SourceExpr exprTrue, SourceExpr exprFalse) {
        super(where, exprTrue, exprFalse);
    }
    ExprCaseList(Where falseWhere) {
        super(falseWhere);
    }

    ExprCase create(Where where, SourceExpr data) {
        return new ExprCase(where,data);
    }

    SourceExpr followWhere(Where where, SourceExpr data, Where upWhere) {
        return data.followFalse(upWhere.or(where.not()));
    }

    // возвращает CaseExpr
    SourceExpr getExpr(Type type) {

        ExprCase lastCase = null;
        // срезаем null'ы
        while(size()>0) {
            lastCase = get(size()-1);
            Where nullWhere = lastCase.data.getWhere().not();
            lastCase.where = lastCase.where.followFalse(nullWhere);
            if(lastCase.where.isFalse()) {
                remove(size()-1);
            } else
                break;
        }

        // если не осталось элементов вернем просто NULL
        if(isEmpty())
            return type.getExpr(null);

        // если остался один его и возвращаем
        if(size()==1 && get(0).where.isTrue())
            return get(0).data;

        return new CaseExpr(this);
    }
}

class MapCase<K> extends Case<Map<K,AndExpr>> {

    MapCase() {
        super(Where.TRUE,new HashMap<K,AndExpr>());
    }

    MapCase(Where iWhere, Map<K, AndExpr> iData) {
        super(iWhere, iData);
    }
}

class MapCaseList<K> extends CaseList<Map<K,AndExpr>,MapCase<K>> {

    MapCaseList() {
    }
    MapCaseList(Where where, Map<K, AndExpr> data) {
        super(where, data);
    }

    MapCase<K> create(Where where, Map<K, AndExpr> data) {
        return new MapCase<K>(where,data);
    }
}


class CaseExpr extends SourceExpr implements CaseWhere<ExprCase> {

    ExprCaseList cases;

    // этот конструктор нужен для создания CaseExpr'а в результать mapCase'а
    CaseExpr(ExprCaseList iCases) {
        cases = iCases;
    }

    CaseExpr(Where where,SourceExpr expr) {
        cases = new ExprCaseList(where,expr);
    }

    CaseExpr(Where where,SourceExpr exprTrue,SourceExpr exprFalse) {
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
        return source + (noElse?"":" ELSE "+Type.NULL)+" END";
    }

    public String toString() {
        String result = "";
        for(ExprCase exprCase : cases)
            result = (result.length()==0?"":result+",")+exprCase.toString();
        return "CE(" + result + ")";
    }

    Type getType() {
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

    SourceExpr followFalse(Where where) {
        if(where.isTrue()) return this;

        ExprCaseList followedCases = new ExprCaseList(where);
        for(ExprCase exprCase : cases)
            followedCases.add(exprCase.where,exprCase.data);
        return followedCases.getExpr(getType());
    }

    static private <K> void recTranslateCase(ListIterator<Map.Entry<K, ? extends SourceExpr>> ic, MapCase<K> current, MapCaseList<K> result, boolean elseCase) {

        if(!ic.hasNext()) {
            result.add(current.where,new HashMap<K,AndExpr>(current.data));
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
    static <K> MapCaseList<K> translateCase(Map<K, ? extends SourceExpr> mapExprs, Translator translator, boolean forceTranslate, boolean elseCase) {
        Map<K,SourceExpr> translateExprs = new HashMap<K,SourceExpr>();
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
    boolean equals(SourceExpr expr, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        if(!(expr instanceof CaseExpr)) return false;

        CaseExpr caseExpr = (CaseExpr) expr;

        if(cases.size()!=caseExpr.cases.size()) return false;

        for(int i=0;i< cases.size();i++)
            if(!cases.get(i).equals(caseExpr.cases.get(i), mapExprs, mapWheres))
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
