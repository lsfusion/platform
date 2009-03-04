package platform.server.data.query.exprs;

import platform.server.data.query.Translator;
import platform.server.data.query.wheres.JoinWhere;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;

import java.util.Map;

public abstract class ObjectExpr extends AndExpr {

    public SourceExpr translate(Translator translator) {
        return translator.translate(this);
    }

    boolean follow(DataWhere where) {
        return false;
    }
    public DataWhereSet getFollows() {
        return new DataWhereSet();
    }

    // для кэша
    public boolean equals(SourceExpr expr, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return mapExprs.get(this).equals(expr);
    }

    int getHash() {
        return 1;
    }
}

