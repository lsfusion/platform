package platformlocal;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

abstract class ObjectExpr extends AndExpr {

    public SourceExpr translate(Translator Translator) {
        return Translator.translate(this);
    }

    boolean follow(DataWhere Where) {
        return false;
    }
    Set<DataWhere> getFollows() {
        return new HashSet<DataWhere>();
    }

    // для кэша
    boolean equals(SourceExpr expr, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        if(mapExprs.get(this)==null)
            throw new RuntimeException("null");
        return mapExprs.get(this).equals(expr);
    }

    int getHash() {
        return 1;
    }
}

class KeyExpr extends ObjectExpr implements QueryData {

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return QueryData.get(this);
    }

    public <J extends Join> void fillJoins(List<J> Joins, Set<ValueExpr> Values) {
    }

    protected void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    Type getType() {
        return Type.Object;
    }

    // возвращает Where без следствий
    Where getWhere() {
        return new AndWhere();
    }
}

class NotNullWhere extends DataWhere {

    JoinExpr Expr;

    NotNullWhere(JoinExpr iExpr) {
        Expr = iExpr;
    }

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return Expr.getSource(QueryData, Syntax) + " IS NOT NULL";
    }

    String getNotSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return Expr.getSource(QueryData, Syntax) + " IS NULL";
    }

    public String toString() {
        return Expr.toString() + " NOT_NULL";
    }

    public Where translate(Translator Translator) {

        SourceExpr TransExpr = Expr.translate(Translator);

        if(TransExpr==Expr)
            return this;

        return TransExpr.getWhere();
    }

    public <J extends Join> void fillJoins(List<J> Joins, Set<ValueExpr> Values) {
        Expr.fillJoins(Joins, Values);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> Joins, Where AndWhere) {
        Expr.fillAndJoinWheres(Joins,AndWhere);
    }

    boolean calculateFollow(DataWhere Where) {
        return Where==this || Expr.follow(Where);
    }
    Set<DataWhere> getFollows() {
        return Expr.getFollows();
    }

    public Where getJoinWhere() {
        return Expr.From.InJoin; // собсно ради этого все и делается
    }

    public Where getNotJoinWhere() {
//        return super.getNotJoinWhere();
        return new AndWhere();
    }

    public Where copy() {
        return this;
    }

    public JoinWheres getInnerJoins() {
        return new JoinWheres(Expr.From.InJoin,this);
    }

    // для кэша
    public boolean equals(Where Where, Map<ObjectExpr, ObjectExpr> MapExprs, Map<JoinWhere, JoinWhere> MapWheres) {
        return Where instanceof NotNullWhere && Expr.equals(((NotNullWhere)Where).Expr,MapExprs, MapWheres);
    }

    public int getHash() {
        return Expr.hash();
    }
}

class JoinExpr<J,U> extends ObjectExpr implements JoinData {
    U Property;
    Join<J,U> From;
    NotNullWhere NotNull;

    JoinExpr(Join<J,U> iFrom,U iProperty) {
        From = iFrom;
        Property = iProperty;
        NotNull = new NotNullWhere(this);
    }

    public <J extends Join> void fillJoins(List<J> Joins, Set<ValueExpr> Values) {
        From.fillJoins(Joins,Values);
    }

    public Join getJoin() {
        return From;
    }

    protected void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(this, andWhere);
    }

    // для fillSingleSelect'а
    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return QueryData.get(this);
    }

    public String toString() {
        return From.toString() + "." + Property;
    }

    Type getType() {
        return From.Source.getType(Property);
    }

    // возвращает Where без следствий
    Where getWhere() {
        return NotNull;
    }

    boolean follow(DataWhere Where) {
        return Where==NotNull || From.InJoin.follow(Where);
    }
    Set<DataWhere> getFollows() {
        Set<DataWhere> Follows = new HashSet<DataWhere>();
        Follows.add(NotNull);
        Follows.addAll(From.InJoin.getFollows());
        return Follows;
    }

    public SourceExpr getFJExpr() {
        return this;
    }

    public String getFJString(String FJExpr) {
        return FJExpr;
    }

    int getHash() {
        return From.hash()*31+From.Source.hashProperty(Property);
    }
}

class NullExpr extends ObjectExpr {

    Type type;
    NullExpr(Type iType) {
        type = iType;
    }

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return Type.NULL;
    }

    public <J extends Join> void fillJoins(List<J> Joins, Set<ValueExpr> Values) {
    }

    protected void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public String toString() {
        return Type.NULL;
    }

    Type getType() {
        return type;
    }

    // возвращает Where на notNull
    Where getWhere() {
        return new OrWhere();
    }

    public boolean equals(Object o) {
        return o instanceof NullExpr;
    }

    public int hashCode() {
        return 0;
    }

    // для кэша
    boolean equals(SourceExpr expr, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return equals(expr);
    }

    int getHash() {
        return hashCode();
    }
}

// формулы
class ValueExpr extends ObjectExpr implements QueryData {

    TypedObject Object;

    ValueExpr(Object Value,Type Type) {
        if(Value==null)
            throw new RuntimeException("use NullExpr");
        Object = new TypedObject(Value,Type);
    }


    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        String Source = QueryData.get(this);
        if(Source==null) Source = getString(Syntax);
        return Source;
//        return getString(Syntax);
    }

    public String getString(SQLSyntax Syntax) {
        return Object.getString(Syntax);
    }

    public String toString() {
        return Object.toString();
    }

    public <J extends Join> void fillJoins(List<J> Joins, Set<ValueExpr> Values) {
        Values.add(this);
    }

    Type getType() {
        return Object.type;
    }

    protected void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    // возвращает Where без следствий
    Where getWhere() {
        return new AndWhere();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValueExpr valueExpr = (ValueExpr) o;

        if (!Object.equals(valueExpr.Object)) return false;

        return true;
    }

    public int hashCode() {
        return Object.hashCode();
    }
}
