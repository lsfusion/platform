package platform.server.data.query.exprs;

import platform.server.data.TypedObject;
import platform.server.data.query.Join;
import platform.server.data.query.JoinData;
import platform.server.data.query.QueryData;
import platform.server.data.query.MapJoinEquals;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.Where;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.Immutable;


public class ValueExpr extends ObjectExpr implements QueryData {

    public final TypedObject object;

    public ValueExpr(Object value, Type type) {
        if(value==null)
            throw new RuntimeException("use NullExpr");
        object = new TypedObject(value,type);
    }


    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        String source = queryData.get(this);
        if(source==null) source = getString(syntax);
        return source;
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
    protected Where calculateWhere() {
        return Where.TRUE;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof ValueExpr && object.equals(((ValueExpr)o).object);
    }

    protected int getHashCode() {
        return object.hashCode();
    }

    // для кэша
    public boolean equals(SourceExpr expr, Map<ValueExpr, ValueExpr> mapValues, Map<KeyExpr, KeyExpr> mapKeys, MapJoinEquals mapJoins) {
        return mapValues.get(this).equals(expr);
    }
}
