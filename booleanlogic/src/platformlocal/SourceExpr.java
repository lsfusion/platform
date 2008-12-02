package platformlocal;

import java.util.*;

// абстрактный класс выражений
abstract class SourceExpr implements SourceJoin {

    String getJoin(String KeySource, Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return KeySource + "=" + getSource(JoinAlias, Syntax);
    }

    abstract Type getType();

    // заменяет Null на Min выражения
    SourceExpr getNullMinExpr() {
        List<Case> Cases = new ArrayList<Case>(getCases());
        Cases.add(new Case(new AndWhere(),getType().getMinValueExpr()));
        return new CaseExpr(Cases);
    }

    abstract SourceExpr translate(ExprTranslator Translator);

    boolean isNull() {return false;}

    // возвращает Where без следствий
    abstract Where getWhere();
    // возвращает Where со следствиями
    abstract Where getFullWhere();

    // получает список Case'ов
    abstract List<Case> getCases();

    static SourceExpr getSum(SourceExpr Sum1,SourceExpr Sum2) {
    }

    static SourceExpr getDiff(SourceExpr From,SourceExpr Diff) {
    }

    static SourceExpr coeff(SourceExpr Expr,Integer Coeff) {

    }
}

abstract class AndExpr extends SourceExpr {

    // получает список Case'ов
    List<Case> getCases() {
        return Collections.singletonList(new Case(new AndWhere(),this));
    }

    // заменяет Null на Min выражения
    SourceExpr getNullMinExpr() {
        CaseExpr Result = new CaseExpr(getWhere(),this);
        Result.Cases.add(new Case(new AndWhere(),getType().getMinValueExpr()));
        return Result;
    }
}

abstract class ObjectExpr extends AndExpr {

    public SourceExpr translate(ExprTranslator Translator) {
        SourceExpr Translated = Translator.Exprs.get(this);
        if(Translated==null) Translated = this;
        return Translated;
    }
}

class KeyExpr<K> extends ObjectExpr {
    K Key;

    KeyExpr(K iKey) {Key=iKey;}

    String Source;
    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Source;
    }

    public void fillJoins(List<Join> Joins) {
    }

    String getJoin(String KeySource, Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        if(Source!=null) return super.getJoin(KeySource,JoinAlias, Syntax);

        Source = KeySource;
        return null;
    }

    Type getType() {
        return Type.Object;
    }

    // возвращает Where без следствий
    Where getWhere() {
        return new AndWhere();
    }

    // возвращает Where на notNull
    Where getFullWhere() {
        // всегда не null
        return new AndWhere();
    }
}

class JoinExpr<J,U> extends ObjectExpr {
    U Property;
    Join<J,U> From;

    JoinExpr(Join<J,U> iFrom,U iProperty) {
        From = iFrom;
        Property = iProperty;
    }

    public void fillJoins(List<Join> Joins) {
        From.fillJoins(Joins);
    }

    // для fillSingleSelect'а
    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return From.Source.getPropertyString(Property,JoinAlias.get(From),Syntax);
    }

    Type getType() {
        return From.Source.getType(Property);
    }

    // возвращает Where без следствий
    Where getWhere() {
        return new NotNullWhere(this);
    }

    // возвращает Where на notNull
    Where getFullWhere() {
        return From.getFullWhere().and(getWhere());
    }
}

// формулы
class ValueExpr extends ObjectExpr {

    TypedObject Object;

    ValueExpr(Object Value,Type Type) {
        Object = new TypedObject(Value,Type);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return getString(Syntax);
    }

    public String getString(SQLSyntax Syntax) {
        return Object.getString(Syntax);
    }

    public void fillJoins(List<Join> Joins) {
    }

    Type getType() {
        return Object.Type;
    }

    boolean isNull() {
        return Object.Value==null;
    }

    // возвращает Where без следствий
    Where getWhere() {
        return (Object.Value==null?new OrWhere():new AndWhere());
    }

    Where getFullWhere() {
        return getWhere();
    }
}

class CaseExpr extends SourceExpr {

    List<Case> Cases;

    CaseExpr(List<Case> iCases) {
        Cases = iCases;
    }

    CaseExpr(Case Case) {
        this(Collections.singletonList(Case));
    }

    CaseExpr(Where Where,AndExpr Expr) {
        this(new Case(Where,Expr));
    }

    // iif
    CaseExpr(Where Where,SourceExpr True,SourceExpr False) {
        Cases = new ArrayList<Case>();
        Cases.add(new Case(Where,True));
        Cases.add(new Case(new AndWhere(),False));
    }


    // получает список Case'ов
    List<Case> getCases() {
        return Cases;
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {

        String Source = "CASE";
        boolean Else = false;
        for(int i=0;i<Cases.size();i++) {
            Case Case = Cases.get(0);
            String CaseSource = Case.Expr.getSource(JoinAlias, Syntax);
            if(i==Cases.size()-1 && Case.Where.isTrue()) {
                Source = Source + " ELSE " + CaseSource;
                Else = true;
            } else
                Source = " WHEN " + Case.Where.getSource(JoinAlias, Syntax) + " THEN " + CaseSource;
        }
        return Source + (Else?"":" ELSE "+Type.NULL)+" END";
    }

    Type getType() {
        return Cases.get(0).Expr.getType();
    }

    public SourceExpr translate(ExprTranslator Translator) {
        boolean ChangedOperands = false;
        List<Case> TranslatedCases = new ArrayList<Case>();
        for(Case Case : Cases) {
            Where TranslatedWhere = Case.Where.translate(Translator);
            SourceExpr TranslatedExpr = Case.Expr.translate(Translator);

            if(Case.Where==TranslatedWhere && Case.Expr==TranslatedExpr)
                TranslatedCases.add(Case);
            else {
                if(TranslatedExpr instanceof AndExpr)
                    TranslatedCases.add(new Case(TranslatedWhere, (AndExpr) TranslatedExpr));
                else { // значит другой Case, кидаем And'ы
                    CaseExpr TranslatedCaseExpr = (CaseExpr)TranslatedExpr;
                    for(Case TranslatedCase : TranslatedCaseExpr.Cases)
                        TranslatedCases.add(new Case(TranslatedWhere.and(TranslatedCase.Where),TranslatedCase.Expr));
                }

                ChangedOperands = true;
            }
        }

        if(!ChangedOperands)
            return this;

        // вообще по хорошему надо повырезать одинаковые
        return new CaseExpr(TranslatedCases);
    }

    public void fillJoins(List<Join> Joins) {
        for(Case Case : Cases) {
            Case.Where.fillJoins(Joins);
            Case.Expr.fillJoins(Joins);
        }
    }

    // строит комбинации из Case'ов
    static <K> List<CaseMap<K>> translateCase(Map<K,? extends SourceExpr> MapExprs,ExprTranslator Translator) {
        Map<K,SourceExpr> TranslateMapExprs = new HashMap<K,SourceExpr>();
        for(Map.Entry<K,? extends SourceExpr> MapExpr : MapExprs.entrySet())
            TranslateMapExprs.put(MapExpr.getKey(),MapExpr.getValue().translate(Translator));
        if(TranslateMapExprs.equals(MapExprs))
            return null;
        else
            return mapCase(TranslateMapExprs);
    }

    // все равно надо выделять
    static <K> List<CaseMap<K>> mapCase(Map<K,SourceExpr> MapExprs) {

    }

    // возвращает Where без следствий
    Where getWhere() {
        List<CaseMap<Integer>> CaseList = mapCase(Collections.singletonMap(0,(SourceExpr)this));
        OrWhere ResultWhere = new OrWhere();
        for(CaseMap<Integer> Case : CaseList)
            ResultWhere.or(Case.FullWhere.and(Case.Map.get(0).getWhere()));
        return ResultWhere;
    }

    static SourceExpr get(Where Where, SourceExpr Expr) {
        if(Expr instanceof AndExpr)
            return new CaseExpr(Collections.singletonList(new Case(Where, (AndExpr) Expr)));

        List<Case> Result = new ArrayList<Case>();
        List<CaseMap<Integer>> CaseList = mapCase(Collections.singletonMap(0, Expr));
        for(CaseMap<Integer> Case : CaseList)
            Result.add(new Case(Case.Where,Case.Map.get(0)));
        return new CaseExpr(Result);
    }

    // возвращает Where на notNull
    Where getFullWhere() {
        OrWhere Result = new OrWhere();
        Where NotWhere = new AndWhere();
        for(Case Case : Cases) {
            Result.or(Case.Where.and(NotWhere).and(Case.Expr.getFullWhere()));
            NotWhere = NotWhere.and(Case.Where.not());
        }
        return Result;
    }
}

class Case {
    Where Where;
    AndExpr Expr;

    Case(Where iWhere, AndExpr iExpr) {
        Where = iWhere;
        Expr = iExpr;
    }
}

class CaseMap<K> {
    Where Where;

    // с ! в том числе
    // на самом деле из List'а можно достать но пока не будем
    Where FullWhere;

    Map<K,AndExpr> Map;
}

class FormulaExpr extends AndExpr {

    String Formula;
    Type DBType;
    Map<String, AndExpr> Params;

    FormulaExpr(String iFormula,Map<String, AndExpr> iParams,Type iDBType) {
        Formula = iFormula;
        Params = iParams;
        DBType = iDBType;
    }

    public void fillJoins(List<Join> Joins) {
        for(SourceExpr Param : Params.values())
            Param.fillJoins(Joins);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {

        String SourceString = Formula;

        for(String Prm : Params.keySet())
            SourceString = SourceString.replace(Prm, Params.get(Prm).getSource(JoinAlias, Syntax));

         return SourceString;
     }

    Type getType() {
        return DBType;
    }

    public SourceExpr translate(ExprTranslator Translator) {
        List<CaseMap<String>> CaseList = CaseExpr.translateCase(Params, Translator);
        if(CaseList==null)
            return this;

        List<Case> Result = new ArrayList<Case>();
        for(CaseMap<String> Case : CaseList)
            Result.add(new Case(Case.Where,new FormulaExpr(Formula,Case.Map,DBType)));
        return new CaseExpr(Result);
    }

    static SourceExpr get(String Formula,Map<String,SourceExpr> Params,Type DBType) {

        List<Case> Result = new ArrayList<Case>();
        List<CaseMap<String>> CaseList = CaseExpr.mapCase(Params);
        for(CaseMap<String> Case : CaseList)
            Result.add(new Case(Case.Where,Case.Map.get(0)));
        return new CaseExpr(Result);
    }


    // возвращает Where без следствий
    Where getWhere() {
        Where Result = new AndWhere();
        for(AndExpr Param : Params.values())
            Result = Result.and(Param.getWhere());
        return Result;
    }

    // возвращает Where на notNull
    Where getFullWhere() {
        Where Result = new AndWhere();
        for(AndExpr Param : Params.values())
            Result = Result.and(Param.getFullWhere());
        return Result;
    }
}
