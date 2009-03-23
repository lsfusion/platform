package platform.server.data.query.exprs;

import platform.server.data.query.Translator;
import platform.server.data.query.MapJoinEquals;
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

    protected int getHash() {
        return 1;
    }
}

