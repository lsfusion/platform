package platform.server.data.query.innerjoins;

import platform.base.BaseUtils;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.JoinSet;
import platform.server.data.where.Where;

import java.util.Set;

public class InnerSelectJoin extends InnerGroupJoin<JoinSet> {
    public InnerSelectJoin(KeyEqual keyEqual, JoinSet joins, Where where) {
        super(keyEqual, joins, where);
    }

    public InnerSelectJoin pack() {
        return new InnerSelectJoin(keyEqual, joins, where.pack());
    }
}
