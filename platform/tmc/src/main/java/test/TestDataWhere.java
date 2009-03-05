package test;

import platform.server.where.DataWhere;
import platform.server.where.Where;
import platform.server.where.DataWhereSet;
import platform.server.data.query.*;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;

class TestDataWhere extends DataWhere {

    String caption;
    TestDataWhere(String iCaption) {
        caption = iCaption;
    }

    public String toString() {
        return caption;
    }

    Set<DataWhere> follows = new HashSet<DataWhere>();

    public boolean follow(DataWhere dataWhere) {
        return equals(dataWhere) || follows.contains(dataWhere);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public JoinWheres getInnerJoins() {
        return new JoinWheres(Where.TRUE,this);
    }

    public boolean equals(Where where, Map<ValueExpr, ValueExpr> mapValues, Map<KeyExpr, KeyExpr> mapKeys, MapJoinEquals mapJoins) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return caption;
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
    }

    protected int getHash() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Where translate(Translator translator) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DataWhereSet getExprFollows() {
        return new DataWhereSet();
    }
}
