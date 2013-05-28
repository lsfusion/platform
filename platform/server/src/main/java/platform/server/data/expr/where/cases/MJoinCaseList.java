package platform.server.data.expr.where.cases;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.query.Join;
import platform.server.data.where.Where;

public class MJoinCaseList<U> extends MCaseList<Join<U>, Join<U>,JoinCase<U>> {

    public ImSet<U> properties;
    public MJoinCaseList(ImSet<U> properties, boolean exclusive) {
        super(exclusive);
        this.properties = properties;
    }

    @Override
    public void add(Where where, Join<U> data) {
        add(new JoinCase<U>(where, data));
    }

    @Override
    public Join<U> getFinal() {
        JoinCaseList<U> finalCases;
        if(exclusive)
            finalCases = new JoinCaseList<U>(immutableSet(), properties);
        else
            finalCases = new JoinCaseList<U>(immutableList(), properties);
        return new CaseJoin<U>(finalCases);
    }


}
