package platform.server.data.where;

import platform.base.BaseUtils;

class Decision {
    private final AndObjectWhere condition;
    private final Where addTrue;
    private final Where addFalse;

    private final OrWhere whereTrue;
    private final OrWhere whereFalse;

    Decision(AndObjectWhere iCondition, Where iAddTrue, Where iAddFalse, OrWhere iWhereTrue, OrWhere iWhereFalse) {
        condition = iCondition;
        addTrue = iAddTrue;
        addFalse = iAddFalse;

        whereTrue = iWhereTrue;
        whereFalse = iWhereFalse;
    }

    Where pairs(Decision decision2, FollowDeep followDeep) {
        if(BaseUtils.hashEquals(condition,decision2.condition))
            return OrWhere.op(OrWhere.op(whereTrue,decision2.addTrue, followDeep).not(),
                OrWhere.op(whereFalse,decision2.addFalse, followDeep).not(), followDeep).not();

        if(BaseUtils.hashEquals(condition,decision2.condition.not()))
            return OrWhere.op(OrWhere.op(whereTrue,decision2.addFalse, followDeep).not(),
                OrWhere.op(whereFalse,decision2.addTrue, followDeep).not(), followDeep).not();

        return null;
    }
}
