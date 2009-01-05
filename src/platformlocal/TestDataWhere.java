package platformlocal;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: NewUser
 * Date: 04.12.2008
 * Time: 11:01:07
 * To change this template use File | Settings | File Templates.
 */
public class TestDataWhere extends DataWhere {

    Set<DataWhere> follows = new HashSet();

    boolean follow(DataWhere dataWhere) {
        if (this.equals(dataWhere)) return true;
        return follows.contains(dataWhere);
    }

    boolean calculateFollow(DataWhere dataWhere) {
        return false;
    }

    IntraWhere translateExpr(Translator translator) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public IntraWhere translate(Translator translator) {
        return null;
    }

    public String getSource(Map<QueryData, String> joinAlias, SQLSyntax syntax) {
        return "";
    }

    public IntraWhere copy() {
        return null;
    }// для кэша

    public boolean equals(IntraWhere where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return false;
    }

    public int hash() {
        return 0;
    }

    IntraWhere translateExpr(ExprTranslator translator) {
        return null;
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, IntraWhere andWhere) {
    }

    public <J extends Join> void fillJoins(List<J> joins) {
    }
}