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
    CaseList(D Data) {
        add(create(Where.TRUE,Data));
    }
    CaseList(Where where,D data) {
        add(create(where, data));
        lastWhere = where;
    }
    CaseList(Where where,D True,D False) {
        super();
        add(create(where,True));
        add(create(Where.TRUE,False));
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
            
            C LastCase = size()>0?get(size()-1):null;
            if(LastCase!=null && LastCase.data.equals(data)) // заOr'им
                LastCase.where = LastCase.where.or(where);
            else
                add(create(where, data));
            lastWhere = where;
        }
    }

    Where getWhere(CaseWhere<C> CaseInterface) {

        Where Result = Where.FALSE;
        Where Up = Where.FALSE;
        for(C Case : this) {
            Where CaseWhere = CaseInterface.getCaseWhere(Case);
            Result = Result.or(Case.where.and(CaseWhere).and(Up.not()));
            Up = Up.or(Case.where);
        }

        return Result;
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
    ExprCaseList(SourceExpr Data) {
        super(Data);
    }
    ExprCaseList(Where Where, SourceExpr Data) {
        super(Where, Data);
    }
    ExprCaseList(Where Where, SourceExpr True, SourceExpr False) {
        super(Where, True, False);
    }
    ExprCaseList(Where falseWhere) {
        super(falseWhere);
    }

    ExprCase create(Where Where, SourceExpr Data) {
        return new ExprCase(Where,Data);
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
    MapCaseList(Where Where, Map<K, AndExpr> Data) {
        super(Where, Data);
    }

    MapCase<K> create(Where Where, Map<K, AndExpr> Data) {
        return new MapCase<K>(Where,Data);
    }
}


class CaseExpr extends SourceExpr implements CaseWhere<ExprCase> {

    ExprCaseList cases;

    // этот конструктор нужен для создания CaseExpr'а в результать mapCase'а
    CaseExpr(ExprCaseList iCases) {
        cases = iCases;
    }

    CaseExpr(Where Where,SourceExpr Expr) {
        cases = new ExprCaseList(Where,Expr);
    }

    CaseExpr(Where Where,SourceExpr True,SourceExpr False) {
        cases = new ExprCaseList(Where,True,False);
    }

/*    CaseExpr(SourceExpr Expr,SourceExpr OpExpr,boolean Sum) {
        Cases = new ExprCaseList(Expr,OpExpr,Sum);
    }*/

    // получает список ExprCase'ов
    ExprCaseList getCases() {
        return cases;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {

        if(cases.size()==0)
            return Type.NULL;
        if(cases.size()==1 && cases.get(0).where.isTrue())
            return cases.get(0).data.getSource(queryData, syntax);

        String Source = "CASE";
        boolean Else = false;
        for(int i=0;i< cases.size();i++) {
            ExprCase Case = cases.get(i);
            String CaseSource = Case.data.getSource(queryData, syntax);

            if(i== cases.size()-1 && Case.where.isTrue()) {
                Source = Source + " ELSE " + CaseSource;
                Else = true;
            } else
                Source = Source + " WHEN " + Case.where.getSource(queryData, syntax) + " THEN " + CaseSource;
        }
        return Source + (Else?"":" ELSE "+Type.NULL)+" END";
    }

    public String toString() {
        String Result = "";
        for(ExprCase Case : cases)
            Result = (Result.length()==0?"":Result+",")+Case.toString();
        return "CE(" + Result + ")";
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
        ExprCaseList followedCases = new ExprCaseList(where);
        for(ExprCase exprCase : cases)
            followedCases.add(exprCase.where,exprCase.data);
        return followedCases.getExpr(getType());
    }

    static private <K> void recTranslateCase(ListIterator<Map.Entry<K, ? extends SourceExpr>> ic, MapCase<K> Current, MapCaseList<K> Result, boolean elseCase) {

        if(!ic.hasNext()) {
            Result.add(Current.where,new HashMap<K,AndExpr>(Current.data));
            return;
        }

        Map.Entry<K,? extends SourceExpr> MapExpr = ic.next();

        for(ExprCase Case : MapExpr.getValue().getCases()) {
            Where PrevWhere = Current.where;
            Current.where = Current.where.and(Case.where);
            Current.data.put(MapExpr.getKey(), (AndExpr) Case.data);
            recTranslateCase(ic,Current,Result, elseCase);
            Current.data.remove(MapExpr.getKey());
            Current.where = PrevWhere;
        }
        if(elseCase)
            recTranslateCase(ic,Current,Result,true);

        ic.previous();
    }

    // строит комбинации из ExprCase'ов
    static <K> MapCaseList<K> translateCase(Map<K, ? extends SourceExpr> MapExprs, Translator Translator, boolean ForceTranslate, boolean elseCase) {
        Map<K,SourceExpr> TranslateExprs = new HashMap<K,SourceExpr>();
        boolean HasCases = false;
        for(Map.Entry<K,? extends SourceExpr> MapExpr : MapExprs.entrySet()) {
            SourceExpr TranslatedExpr = MapExpr.getValue().translate(Translator);
            TranslateExprs.put(MapExpr.getKey(), TranslatedExpr);
            HasCases = HasCases || TranslatedExpr instanceof CaseExpr;
        }

        if(!ForceTranslate && !HasCases && TranslateExprs.equals(MapExprs))
            return null;
        else {
            MapCaseList<K> Result = new MapCaseList<K>();
            recTranslateCase(new ArrayList<Map.Entry<K,? extends SourceExpr>>(TranslateExprs.entrySet()).listIterator(),new MapCase<K>(),Result, elseCase);
            return Result;
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

        CaseExpr CaseExpr = (CaseExpr) expr;

        if(cases.size()!=CaseExpr.cases.size()) return false;

        for(int i=0;i< cases.size();i++)
            if(!cases.get(i).equals(CaseExpr.cases.get(i), mapExprs, mapWheres))
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
