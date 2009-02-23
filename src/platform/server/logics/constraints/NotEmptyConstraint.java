package platform.server.logics.constraints;

import platform.server.data.query.wheres.CompareWhere;

// <= 0 или <= ''
class NotEmptyConstraint extends ValueConstraint {
    NotEmptyConstraint() {super(CompareWhere.LESS_EQUALS);}
}
