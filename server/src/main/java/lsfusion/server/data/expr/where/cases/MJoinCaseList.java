package lsfusion.server.data.expr.where.cases;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.where.Where;

public class MJoinCaseList<U> extends MCaseList<Join<U>, Join<U>,JoinCase<U>> {

    public ImSet<U> properties;
    public MJoinCaseList(ImSet<U> properties, boolean exclusive) {
        super(exclusive);
        this.properties = properties;
    }

    @Override
    public void add(Where where, Join<U> data) {
        add(new JoinCase<>(where, data));
    }

    @Override
    public Join<U> getFinal() {
        JoinCaseList<U> finalCases;
        if(exclusive)
            finalCases = new JoinCaseList<>(immutableSet(), properties);
        else
            finalCases = new JoinCaseList<>(immutableList(), properties);
        return new CaseJoin<>(finalCases);
    }


}
