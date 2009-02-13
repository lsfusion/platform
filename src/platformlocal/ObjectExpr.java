package platformlocal;

import java.util.Map;
import java.util.List;
import java.util.Set;

abstract class ObjectExpr extends AndExpr {

    public SourceExpr translate(Translator translator) {
        return translator.translate(this);
    }

    boolean follow(DataWhere Where) {
        return false;
    }
    DataWhereSet getFollows() {
        return new DataWhereSet();
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

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return queryData.get(this);
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
    }

    protected void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    Type getType() {
        return Type.Object;
    }

    // возвращает Where без следствий
    Where getWhere() {
        return Where.TRUE;
    }
}

class NotNullWhere extends DataWhere {

    JoinExpr Expr;

    NotNullWhere(JoinExpr iExpr) {
        Expr = iExpr;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return Expr.getSource(queryData, syntax) + " IS NOT NULL";
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

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        Expr.fillJoins(joins, values);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> Joins, Where AndWhere) {
        Expr.fillAndJoinWheres(Joins,AndWhere);
    }

    DataWhereSet getExprFollows() {
        return Expr.from.inJoin.getFollows();
    }

    public Where getJoinWhere() {
        return Expr.from.inJoin; // собсно ради этого все и делается
    }

    public Where getNotJoinWhere() {
//        return super.getNotJoinWhere();
        return Where.TRUE;
    }

    public Where copy() {
        return this;
    }

    public JoinWheres getInnerJoins() {
        return new JoinWheres(Expr.from.inJoin,this);
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
    Join<J,U> from;
    NotNullWhere NotNull;

    JoinExpr(Join<J,U> iFrom,U iProperty) {
        from = iFrom;
        Property = iProperty;
        NotNull = new NotNullWhere(this);
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        from.fillJoins(joins, values);
    }

    public Join getJoin() {
        return from;
    }

    protected void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(this, andWhere);
    }

    // для fillSingleSelect'а
    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return queryData.get(this);
    }

    public String toString() {
        return from.toString() + "." + Property;
    }

    Type getType() {
        return from.source.getType(Property);
    }

    // возвращает Where без следствий
    Where getWhere() {
        return NotNull;
    }

    boolean follow(DataWhere Where) {
        return Where==NotNull || from.inJoin.follow(Where);
    }
    DataWhereSet getFollows() {
        return NotNull.getFollows();
    }

    public SourceExpr getFJExpr() {
        return this;
    }

    public String getFJString(String exprFJ) {
        return exprFJ;
    }

    int getHash() {
        return from.hash()*31+ from.source.hashProperty(Property);
    }
}

class NullExpr extends ObjectExpr {

    Type type;
    NullExpr(Type iType) {
        type = iType;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return Type.NULL;
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
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
        return Where.FALSE;
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

    // получает список ExprCase'ов
    ExprCaseList getCases() {
        return new ExprCaseList();
    }
}

// формулы
class ValueExpr extends ObjectExpr implements QueryData {

    TypedObject object;

    ValueExpr(Object Value,Type Type) {
        if(Value==null)
            throw new RuntimeException("use NullExpr");
        object = new TypedObject(Value,Type);
    }


    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        String Source = queryData.get(this);
        if(Source==null) Source = getString(syntax);
        return Source;
//        return getString(Syntax);
    }

    public String getString(SQLSyntax Syntax) {
        return object.getString(Syntax);
    }

    public String toString() {
        return object.toString();
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        values.add(this);
    }

    Type getType() {
        return object.type;
    }

    protected void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    // возвращает Where без следствий
    Where getWhere() {
        return Where.TRUE;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValueExpr valueExpr = (ValueExpr) o;

        if (!object.equals(valueExpr.object)) return false;

        return true;
    }

    public int hashCode() {
        return object.hashCode();
    }
}
