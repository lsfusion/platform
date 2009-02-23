package platform.server.logics.constraints;

import platform.server.data.query.wheres.CompareWhere;

// < 0
class PositiveConstraint extends ValueConstraint {
    PositiveConstraint() {super(CompareWhere.LESS);}
}
