package platform.server.where;

import net.jcip.annotations.Immutable;


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

    Where pairs(Decision decision2,boolean plainFollow) {
        if(condition.hashEquals(decision2.condition))
            return OrWhere.op(OrWhere.op(whereTrue,decision2.addTrue,plainFollow).not(),
                OrWhere.op(whereFalse,decision2.addFalse,plainFollow).not(),plainFollow).not();

        if(condition.hashEquals(decision2.condition.not()))
            return OrWhere.op(OrWhere.op(whereTrue,decision2.addFalse,plainFollow).not(),
                OrWhere.op(whereFalse,decision2.addTrue,plainFollow).not(),plainFollow).not();

        return null;
    }
}
