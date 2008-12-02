package platformlocal;

import java.util.*;

interface Where extends SourceJoin {

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax);
    Where translate(ExprTranslator Translator);

    OrWhere getOr();

    Where not();

    boolean isFalse();
    boolean isTrue();

    abstract Where and(Where Where);

    // возвращает в том числе все следствия
    abstract Where getFullWhere();
}

abstract class ObjectWhere implements Where {

    static String TRUE = "1=1";
    static String FALSE = "1<>1";

    public boolean isFalse() {
        return false;
    }

    public OrWhere getOr() {
        return new OrWhere(this);
    }

    public boolean isTrue() {
        return false;
    }

    public Where and(Where Where) {
        if(Where instanceof ObjectWhere) {
            return new AndWhere(this).and(new AndWhere((ObjectWhere) Where));
        } else
            return Where.and(this);
    }
}

abstract class DataWhere extends ObjectWhere {

    public Where not() {
        return new NotWhere(this);
    }
}

class JoinWhere extends DataWhere {
    Join From;

    JoinWhere(Join iFrom) {
        From=iFrom;
    }

    public void fillJoins(List<Join> Joins) {
        From.fillJoins(Joins);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return From.Source.getInSelectString(JoinAlias.get(From)) + " NOT IS NULL ";
    }

    public Where translate(ExprTranslator Translator) {
        Where Translated = Translator.Wheres.get(this);
        if(Translated==null) Translated = this;
        return Translated;
    }

    // возвращает в том числе все следствия
    public Where getFullWhere() {
        return From.getFullWhere().and(this);
    }
}

class OrWhere extends GraphNodeSet<AndWhere,OrWhere> implements Where {

    void or(Where Where) {
        if(Where instanceof AndWhere)
            or((AndWhere)Where);
        else
        if(Where instanceof OrWhere)
            or((OrWhere)Where);
        else
            or(new AndWhere((ObjectWhere) Where));
    }

    OrWhere() {
    }

    OrWhere(AndWhere Where) {
        super(Collections.singleton(Where));
    }

    OrWhere(Set<AndWhere> iNodes) {
        super(iNodes);
    }

    Set<AndWhere> and(AndWhere AndNode, AndWhere Node) {
        return Collections.singleton(AndNode.and(Node));
    }

    OrWhere create(Set<AndWhere> iNodes) {
        return new OrWhere(iNodes);
    }

    OrWhere(ObjectWhere Where) {
        this(new AndWhere(Where));
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        if(isEmpty())
            return "1<>1";

        if(size()==1)
            return iterator().next().getSource(JoinAlias, Syntax);

        String Source = "";
        for(AndWhere Where : this)
            Source = (Source.length()==0?"":Source+" OR ") + Where.getSource(JoinAlias, Syntax);
        return Source;
    }

    public void fillJoins(List<Join> Joins) {
        for(AndWhere Where : this)
            Where.fillJoins(Joins);
    }

    public Where translate(ExprTranslator Translator) {
        // сначала транслируем AndWhere
        Collection<Where> TransWheres = new ArrayList<Where>();
        boolean ChangedWheres = false;
        for(AndWhere Where : this) {
            Where TransWhere = Where.translate(Translator);
            TransWheres.add(TransWhere);
            ChangedWheres = ChangedWheres || (TransWhere!=Where);
        }

        if(!ChangedWheres)
            return this;

        OrWhere TransOr = new OrWhere();
        for(Where Where : TransWheres)
            TransOr.or(Where);
        return TransOr;
    }

    boolean has(AndWhere OrNode, AndWhere Node) {
        return OrNode.containsAll(Node);
    }

    public Where not() {
        Where Result = new AndWhere();
        for(AndWhere Where : this)
            Result = Result.and(Where.not());
        return Result;
    }

    // возвращает в том числе все следствия
    public Where getFullWhere() {
        OrWhere Result = new OrWhere();
        for(AndWhere Where : this)
            Result.or(Where.getFullWhere());
        return Result;
    }

    public boolean isFalse() {
        return isEmpty();
    }

    public boolean isTrue() {
        return false;
    }

    public Where and(Where Where) {
        if(Where instanceof AndWhere)
            return and((AndWhere)Where);
        else
            return and(Where);
    }

    public OrWhere getOr() {
        return this;
    }
}

class AndWhere extends HashSet<ObjectWhere> implements Where {

    AndWhere() {
    }

    AndWhere(ObjectWhere Where) {
        super(Collections.singleton(Where));
    }

    AndWhere(AndWhere Where) {
        super(Where);
    }

    public OrWhere getOr() {
        return new OrWhere(this);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        if(isEmpty())
            return "1=1";

        if(size()==1)
            return iterator().next().getSource(JoinAlias, Syntax);

        String Source = "";
        for(ObjectWhere Where : this)
            Source = (Source.length()==0?"":Source+" AND ") + Where.getSource(JoinAlias, Syntax);
        return Source;
    }

    public void fillJoins(List<Join> Joins) {
        for(ObjectWhere Where : this)
            Where.fillJoins(Joins);
    }

    public Where translate(ExprTranslator Translator) {
        // сначала транслируем AndWhere
        Collection<Where> TransWheres = new ArrayList<Where>();
        boolean ChangedWheres = false;
        for(ObjectWhere Where : this) {
            Where TransWhere = Where.translate(Translator);
            TransWheres.add(TransWhere);
            ChangedWheres = ChangedWheres || (TransWhere!=Where);
        }

        if(!ChangedWheres)
            return this;

        Where Trans = new AndWhere();
        for(Where Where : TransWheres)
            Trans = Trans.and(Where);
        return Trans;
    }

    AndWhere and(AndWhere Op) {
        AndWhere Result = new AndWhere(this);
        Result.addAll(Op);
        return Result;
    }

    public Where and(Where Where) {
        if(Where instanceof AndWhere)
            return and((AndWhere)Where);
        else
        if(Where instanceof ObjectWhere)
            return and(new AndWhere((ObjectWhere)Where));
        else // тогда уже OR
            return Where.and(this);
    }

    public Where not() {
        OrWhere Result = new OrWhere();
        for(ObjectWhere Where : this)
            Result.or(Where.not());
        return Result;
    }

    public boolean isFalse() {
        return false;
    }

    public boolean isTrue() {
        return isEmpty();
    }

    // возвращает в том числе все следствия
    public Where getFullWhere() {
        Where Result = new AndWhere();
        for(ObjectWhere Where : this)
            Result = Result.and(Where.getFullWhere());
        return Result;
    }

}

class CompareWhere extends DataWhere {

    AndExpr Operator1;
    AndExpr Operator2;

    static int EQUALS = 0;
    static int GREATER = 1;
    static int LESS = 2;
    static int GREATER_EQUALS = 3;
    static int LESS_EQUALS = 4;
    static int NOT_EQUALS = 5;

    int Compare;

    CompareWhere(AndExpr iOperator1,AndExpr iOperator2,int iCompare) {
        Operator1 = iOperator1;
        Operator2 = iOperator2;
        Compare = iCompare;
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Operator1.getSource(JoinAlias, Syntax) + (Compare==EQUALS?"=":(Compare==GREATER?">":(Compare==LESS?"<":(Compare==GREATER_EQUALS?">=":(Compare==LESS_EQUALS?"<=":"<>"))))) + Operator2.getSource(JoinAlias, Syntax);
    }

    public Where translate(ExprTranslator Translator) {

        Map<Integer,SourceExpr> MapExprs = new HashMap<Integer, SourceExpr>();
        MapExprs.put(1,Operator1);
        MapExprs.put(2,Operator2);
        List<CaseMap<Integer>> CaseList = CaseExpr.translateCase(MapExprs,Translator);

        if(CaseList==null)
            return this;

        // значит Case вытаскиваем его наверх
        OrWhere TranslatedWhere = new OrWhere();
        for(CaseMap<Integer> Case : CaseList)
            TranslatedWhere.or(Case.FullWhere.and(new CompareWhere(Case.Map.get(1),Case.Map.get(2),Compare)));
        return TranslatedWhere;
    }

    static Where get(SourceExpr Operator1,SourceExpr Operator2,int Compare) {

    }

    // возвращает в том числе все следствия
    public Where getFullWhere() {
        return Operator1.getFullWhere().and(Operator2.getFullWhere()).and(this);
    }

    public void fillJoins(List<Join> Joins) {
        Operator1.fillJoins(Joins);
        Operator2.fillJoins(Joins);
    }
}

class NotNullWhere extends DataWhere {

    JoinExpr Expr;

    NotNullWhere(JoinExpr iExpr) {
        Expr = iExpr;
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Expr.getSource(JoinAlias, Syntax) + " IS NOT NULL";
    }

    public Where translate(ExprTranslator Translator) {

        SourceExpr TransExpr = Expr.translate(Translator);

        if(TransExpr==Expr)
            return this;

        return TransExpr.getWhere();
    }

    public void fillJoins(List<Join> Joins) {
        Expr.fillJoins(Joins);
    }

    // возвращает в том числе все следствия
    public Where getFullWhere() {
        return Expr.getFullWhere().and(this);
    }
}

class NotWhere extends ObjectWhere {

    DataWhere Where;

    NotWhere(DataWhere iWhere) {
        Where = iWhere;
    }

    final static String PREFIX = "NOT ";

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return PREFIX + Where.getSource(JoinAlias, Syntax);
    }

    public Where translate(ExprTranslator Translator) {
        Where TranslatedWhere = Where.translate(Translator);
        if(TranslatedWhere==Where)
            return this;

        return TranslatedWhere.not();
    }

    public Where not() {
        return Where;
    }

    public void fillJoins(List<Join> Joins) {
        Where.fillJoins(Joins);
    }

    // возвращает в том числе все следствия
    public Where getFullWhere() {
        return Where.getFullWhere().not();
    }
}
