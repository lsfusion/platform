package platform.server.where;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;

import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.ObjectExpr;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.query.wheres.JoinWhere;
import platform.server.data.query.*;
import platform.server.data.sql.SQLSyntax;

class NotWhere extends ObjectWhere<DataWhere> {

    DataWhere where;
    NotWhere(DataWhere iWhere) {
        where = iWhere;
    }

    public ObjectWhereSet calculateObjects() {
        return new ObjectWhereSet(this);
    }

    public boolean directMeansFrom(AndObjectWhere meanWhere) {
        return meanWhere instanceof NotWhere && where.follow(((NotWhere)meanWhere).where);
    }

    public DataWhere getNot() {
        return where;
    }

    static String PREFIX = "NOT ";
    public String toString() {
        return PREFIX+where;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof NotWhere && where.equals(((NotWhere)o).where);
    }

    public int hashCode() {
        return where.hashCode()*31;
    }

    public boolean evaluate(Collection<DataWhere> data) {
        return !where.evaluate(data);
    }

    public Where decompose(ObjectWhereSet decompose, ObjectWhereSet objects) {
        if(where.getFollows().intersect(decompose.data))
            return TRUE;
        else {
            objects.not.add(where);
            objects.followNot.addAll(where.getFollows());
            return this;
        }
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public Where translate(Translator translator) {
        Where transWhere = where.translate(translator);
        if(transWhere==where)
            return this;
        return transWhere.not();
    }


    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        where.fillJoins(joins,values);
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return where.getNotSource(queryData,syntax);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        where.fillDataJoinWheres(joins, andWhere);
    }

    public JoinWheres getInnerJoins() {
        return new JoinWheres(TRUE,this);
    }

    public boolean equals(Where equalWhere, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return equalWhere instanceof NotWhere && where.equals(((NotWhere)equalWhere).where, mapExprs, mapWheres) ;
    }

    public int getHash() {
        return where.hash()*3;
    }
}
