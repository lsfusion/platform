package lsfusion.server.data.where;

import lsfusion.base.BaseUtils;

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

    Where pairs(Decision decision2) {
        if(BaseUtils.hashEquals(condition,decision2.condition))
            return OrWhere.orPairs(OrWhere.orPairs(whereTrue,decision2.addTrue).not(),
                OrWhere.orPairs(whereFalse,decision2.addFalse).not()).not();

        if(BaseUtils.hashEquals(condition,decision2.condition.not()))
            return OrWhere.orPairs(OrWhere.orPairs(whereTrue,decision2.addFalse).not(),
                OrWhere.orPairs(whereFalse,decision2.addTrue).not()).not();

        return null;
    }
}
