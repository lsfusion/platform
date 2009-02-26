package platform.server.data.query.exprs;

import platform.server.data.TypedObject;
import platform.server.data.query.Join;
import platform.server.data.query.JoinData;
import platform.server.data.query.QueryData;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.Where;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ValueExpr extends ObjectExpr implements QueryData {

    public TypedObject object;

    public ValueExpr(Object Value, Type Type) {
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

    public Type getType() {
        return object.type;
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    // возвращает Where без следствий
    Where calculateWhere() {
        return Where.TRUE;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof ValueExpr && object.equals(((ValueExpr)o).object);
    }

    public int hashCode() {
        return object.hashCode();
    }
}
