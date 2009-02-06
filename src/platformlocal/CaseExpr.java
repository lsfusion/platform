package platformlocal;

import java.util.*;

abstract class Case<D> {
    Where Where;
    D Data;

    Case(Where iWhere,D iData) {
        Where = iWhere;
        Data = iData; 
    }
}

interface CaseWhere<C> {
    Where getCaseWhere(C Case);
}

abstract class CaseList<D,C extends Case<D>> extends ArrayList<C> {

    CaseList() {
    }
    CaseList(D Data) {
        add(create(new AndWhere(),Data));
    }
    CaseList(Where Where,D Data) {
        add(create(Where,Data));
        LastWhere = Where;
    }
    CaseList(Where Where,D True,D False) {
        super();
        add(create(Where,True));
        add(create(new AndWhere(),False));
    }

    Where PrevUpWhere = new OrWhere();
    Where LastWhere = null;
    Where getUpWhere() { // для оптимизации последних элементов
        if(LastWhere!=null) {
            PrevUpWhere = PrevUpWhere.or(LastWhere);
            LastWhere = null;
        }

        return PrevUpWhere;
    }

    // добавляет Case, проверяя все что можно
    void add(Where Where,D Data) {

        Where UpWhere = getUpWhere();
//        if(Where.getSize()+UpWhere.getSize()>25)
//            Where = Where;
        Where = Where.followFalse(UpWhere);
        if(!Where.isFalse()) {
            C LastCase = size()>0?get(size()-1):null;
            if(LastCase!=null && LastCase.Data.equals(Data)) // заOr'им
                LastCase.Where = LastCase.Where.or(Where);
            else
                add(create(Where,Data));
            LastWhere = Where;
        }
    }

    Where getWhere(CaseWhere<C> CaseInterface) {

        if(size()>10)
            LastWhere = LastWhere;

        Where Result = new OrWhere();
        Where Up = new OrWhere();
        for(C Case : this) {
            Where CaseWhere = CaseInterface.getCaseWhere(Case);
            Result = Result.or(Case.Where.and(CaseWhere).and(Up.not()));
            Up = Up.or(Case.Where);
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

//        if(!iWhere.means(iExpr.getWhere()))
//            iWhere = iWhere;
    }

    public String toString() {
        return Where.toString() + "-" + Data.toString();
    }

    // для кэша
    boolean equals(ExprCase Case, Map<ObjectExpr, ObjectExpr> MapExprs, Map<JoinWhere, JoinWhere> MapWheres) {
        return Where.equals(Case.Where, MapExprs, MapWheres) && Data.equals(Case.Data, MapExprs, MapWheres);
    }

    int hash() {
        return Where.hash()*31+Data.hash();
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
/*    ExprCaseList(SourceExpr Expr,SourceExpr OpExpr,boolean Sum) {
        add(new ExprCase(Expr.getWhere().and(OpExpr.getWhere()),new FormulaExpr(Expr,OpExpr,Sum)));
        add(new ExprCase(Expr.getWhere(),Expr));
        add(new ExprCase(new AndWhere(),OpExpr));
    }*/

    Type Type = null;
    void add(Where Where, SourceExpr Data) {
        if(Type==null) Type = Data.getType(); // получим тип на всякий случай
        super.add(Where, Data);
    }

    ExprCase create(Where Where, SourceExpr Data) {
        return new ExprCase(Where,Data);
    }

    // возвращает CaseExpr
    SourceExpr getExpr() {

        if(size()>10)
            LastWhere = LastWhere;

        ExprCase LastCase = null;
        // срезаем null'ы
        while(size()>0) {
            LastCase = get(size()-1);
            Where NullWhere = LastCase.Data.getWhere().not();
            LastCase.Where = LastCase.Where.followFalse(NullWhere);
            if(LastCase.Where.isFalse()) {
                remove(size()-1);
            } else
                break;
        }

        // если не осталось элементов вернем просто NULL
        if(isEmpty())
            return new NullExpr(LastCase==null?Type:LastCase.Data.getType());

        // если остался один его и возвращаем
        if(size()==1 && get(0).Where.isTrue())
            return get(0).Data;

        return new CaseExpr(this);
    }
}

class MapCase<K> extends Case<Map<K,AndExpr>> {

    MapCase() {
        super(new AndWhere(),new HashMap<K,AndExpr>());
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

    ExprCaseList Cases;

    // этот конструктор нужен для создания CaseExpr'а в результать mapCase'а
    CaseExpr(ExprCaseList iCases) {
        Cases = iCases;
    }

    CaseExpr(Where Where,SourceExpr Expr) {
        Cases = new ExprCaseList(Where,Expr);
    }

    CaseExpr(Where Where,SourceExpr True,SourceExpr False) {
        Cases = new ExprCaseList(Where,True,False);
    }

/*    CaseExpr(SourceExpr Expr,SourceExpr OpExpr,boolean Sum) {
        Cases = new ExprCaseList(Expr,OpExpr,Sum);
    }*/

    // получает список ExprCase'ов
    ExprCaseList getCases() {
        return Cases;
    }

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {

        if(Cases.size()==0)
            return Type.NULL;
        if(Cases.size()==1 && Cases.get(0).Where.isTrue())
            return Cases.get(0).Data.getSource(QueryData, Syntax);

        String Source = "CASE";
        boolean Else = false;
        for(int i=0;i<Cases.size();i++) {
            ExprCase Case = Cases.get(i);
            String CaseSource = Case.Data.getSource(QueryData, Syntax);
            if(i==Cases.size()-1 && Case.Where.isTrue()) {
                Source = Source + " ELSE " + CaseSource;
                Else = true;
            } else
                Source = Source + " WHEN " + Case.Where.getSource(QueryData, Syntax) + " THEN " + CaseSource;
        }
        return Source + (Else?"":" ELSE "+Type.NULL)+" END";
    }

    public String toString() {
        String Result = "";
        for(ExprCase Case : Cases)
            Result = (Result.length()==0?"":Result+",")+Case.toString();
        return "CE(" + Result + ")";
    }

/*    SourceExpr compile(Where QueryWhere) {

        if(getComplexity()>90)
            Cases = Cases;

        // устраняем одинаковые AndExpr'ы
        List<ExprCase> MergedCases = new ArrayList<ExprCase>();
        while(ProceedCases.size()>0) {
            Iterator<ExprCase> itCase = ProceedCases.iterator();
            ExprCase ExprCase = itCase.next();
            itCase.remove();

            OrWhere Equal = new OrWhere();
            Equal.or(ExprCase.Where);

            OrWhere NotEqual = new OrWhere();
            while(itCase.hasNext()) {
                ExprCase CheckCase = itCase.next();

                if(CheckCase.Expr.equals(ExprCase.Expr)) {
                    Equal.or(CheckCase.Where.andNot(NotEqual));
                    itCase.remove();
                } else
                    NotEqual.or(CheckCase.Where);
            }

            MergedCases.add(new ExprCase(Equal,ExprCase.Expr));
        }
    }
     */
    Type getType() {
        return Cases.get(0).Data.getType();
    }

    // не means OR верхних +
    // нету NullExpr'ов в ExprCase'е +
    // нету одинаковых AndExpr'ов 
    // пустой ExprCase - null
    // из одного элемента который means Expr.getWhere - по сути достаточно чистого Expr'а (не обязательно)
    // вообще разделение на translate и compile такое что все что быстро выполняется лучше сюда, остальное в Compile

    // все без not'ов (means,andNot)
    public SourceExpr translate(Translator Translator) {

        ExprCaseList TranslatedCases = new ExprCaseList();
        for(Iterator<ExprCase> i=Cases.iterator();i.hasNext();) {
            ExprCase Case = i.next();
            Where TranslatedWhere = Case.Where.translate(Translator);
            for(ExprCase TranslatedCase : Case.Data.translate(Translator).getCases()) // здесь на самом деле заведомо будут AndExpr'ы
                TranslatedCases.add(TranslatedWhere.and(TranslatedCase.Where),TranslatedCase.Data);
            if(i.hasNext()) // для оптимизации последнюю не добавлять  && !TranslatedData.get(TranslatedData.size()-1).Where.isTrue()
                TranslatedCases.add(TranslatedWhere,new NullExpr(getType()));
        }

        return TranslatedCases.getExpr();
    }
    
    static private <K> void recTranslateCase(ListIterator<Map.Entry<K,? extends SourceExpr>> ic,MapCase<K> Current,MapCaseList<K> Result) {

        if(!ic.hasNext()) {
            Result.add(Current.Where,new HashMap<K,AndExpr>(Current.Data));
            return;
        }

        Map.Entry<K,? extends SourceExpr> MapExpr = ic.next();

        for(ExprCase Case : MapExpr.getValue().getCases()) {
            Where PrevWhere = Current.Where;
            Current.Where = Current.Where.and(Case.Where);
            Current.Data.put(MapExpr.getKey(), (AndExpr) Case.Data);
            recTranslateCase(ic,Current,Result);
            Current.Where = PrevWhere;
        }

        ic.previous();
    }

    // строит комбинации из ExprCase'ов
    static <K> MapCaseList<K> translateCase(Map<K,? extends SourceExpr> MapExprs, Translator Translator,boolean ForceTranslate) {
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
            recTranslateCase(new ArrayList<Map.Entry<K,? extends SourceExpr>>(TranslateExprs.entrySet()).listIterator(),new MapCase<K>(),Result);
            return Result;
        }
    }

    public <J extends Join> void fillJoins(List<J> Joins, Set<ValueExpr> Values) {
        for(ExprCase Case : Cases) {
            Case.Where.fillJoins(Joins, Values);
            Case.Data.fillJoins(Joins, Values);
        }
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        // здесь по-хорошему надо andNot(верхних) но будет тормозить
        for(ExprCase Case : Cases) {
            Case.Where.fillJoinWheres(joins, andWhere);
            Case.Data.fillJoinWheres(joins, andWhere.and(Case.Where));
        }
    }

    public Where getCaseWhere(ExprCase Case) {
        return Case.Data.getWhere();
    }

    // возвращает Where без следствий
    Where getWhere() {
        return Cases.getWhere(this);
    }

    // для кэша
    boolean equals(SourceExpr expr, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        if(!(expr instanceof CaseExpr)) return false;

        CaseExpr CaseExpr = (CaseExpr) expr;

        if(Cases.size()!=CaseExpr.Cases.size()) return false;

        for(int i=0;i<Cases.size();i++)
            if(!Cases.get(i).equals(CaseExpr.Cases.get(i), mapExprs, mapWheres))
                return false;

        return true;
    }

    int getHash() {
        int Hash = 0;
        for(ExprCase Case : Cases)
            Hash = 31*Hash + Case.hash();    
        return Hash;
    }
}
