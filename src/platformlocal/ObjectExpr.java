package platformlocal;

import java.util.Map;
import java.util.List;

abstract class ObjectExpr extends AndExpr {

    public SourceExpr translate(Translator Translator) {
        return this;
    }

    boolean follow(DataWhere Where) {
        return false;
    }

    // для кэша
    boolean equals(SourceExpr Expr, Map<ObjectExpr, ObjectExpr> MapExprs, Map<JoinWhere, JoinWhere> MapWheres) {
        return MapExprs.get(this) == Expr;
    }

    int hash() {
        return 1;
    }
}

class KeyExpr<K> extends ObjectExpr implements QueryData {
    K Key;

    KeyExpr(K iKey) {Key=iKey;}

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return QueryData.get(this);
    }

    public <J extends Join> void fillJoins(List<J> Joins) {
    }

    protected void fillAndJoinWheres(MapWhere<JoinData> Joins, IntraWhere AndWhere) {
    }

    Type getType() {
        return Type.Object;
    }

    // возвращает IntraWhere без следствий
    IntraWhere getWhere() {
        return new InnerWhere();
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

    public IntraWhere translate(Translator Translator) {

        SourceExpr TransExpr = Translator.translate(Expr);

        if(TransExpr==Expr)
            return this;

        return TransExpr.getWhere();
    }

    public <J extends Join> void fillJoins(List<J> Joins) {
        Expr.fillJoins(Joins);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> Joins, IntraWhere AndWhere) {
        Expr.fillAndJoinWheres(Joins,AndWhere);
    }

    boolean calculateFollow(DataWhere Where) {
        return Where==this || Expr.follow(Where);
    }

    public IntraWhere getJoinWhere() {
        return Expr.From.InJoin; // собсно ради этого все и делается
    }

    public IntraWhere getNotJoinWhere() {
//        return super.getNotJoinWhere();
        return new InnerWhere();
    }

    public IntraWhere copy() {
        return this;
    }

    // для кэша
    public boolean equals(IntraWhere Where, Map<ObjectExpr, ObjectExpr> MapExprs, Map<JoinWhere, JoinWhere> MapWheres) {
        return Where instanceof NotNullWhere && Expr.equals(((NotNullWhere)Where).Expr,MapExprs, MapWheres);
    }

    public int hash() {
        return Expr.hash();
    }
}

class JoinExpr<J,U> extends ObjectExpr implements JoinData {
    U Property;
    Join<J,U> From;
    IntraWhere NotNull;

    JoinExpr(Join<J,U> iFrom,U iProperty) {
        From = iFrom;
        Property = iProperty;
        NotNull = new NotNullWhere(this);
    }

    public <J extends Join> void fillJoins(List<J> Joins) {
        From.fillJoins(Joins);
    }

    public Join getJoin() {
        return From;
    }

    protected void fillAndJoinWheres(MapWhere<JoinData> Joins, IntraWhere AndWhere) {
        Joins.add(this,AndWhere);
    }

    // для fillSingleSelect'а
    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return QueryData.get(this);
    }

    public String toString() {
        return From.toString() + "." + From.Source.getPropertyName(Property);
    }

    Type getType() {
        return From.Source.getType(Property);
    }

    // возвращает IntraWhere без следствий
    IntraWhere getWhere() {
        return NotNull;
    }

    boolean follow(DataWhere Where) {
        return Where==NotNull || From.InJoin.follow(Where);
    }

    public SourceExpr getFJExpr() {
        return this;
    }

    public String getFJString(String FJExpr) {
        return FJExpr;
    }

    int hash() {
        return From.hash();
    }
}

// формулы
class ValueExpr extends ObjectExpr {

    TypedObject Object;

    ValueExpr(Object Value,Type Type) {
        Object = new TypedObject(Value,Type);
    }

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return getString(Syntax);
    }

    public String getString(SQLSyntax Syntax) {
        return Object.getString(Syntax);
    }

    public String toString() {
        return Object.toString();
    }

    public <J extends Join> void fillJoins(List<J> Joins) {
    }

    protected void fillAndJoinWheres(MapWhere<JoinData> Joins, IntraWhere AndWhere) {
    }

    Type getType() {
        return Object.type;
    }

    boolean isNull() {
        return Object.Value==null;
    }

    // возвращает IntraWhere без следствий
    IntraWhere getWhere() {
        return (Object.Value==null?new OuterWhere():new InnerWhere());
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
