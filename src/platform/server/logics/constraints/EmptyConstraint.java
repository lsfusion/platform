package platform.server.logics.constraints;

import platform.server.data.query.wheres.CompareWhere;

// != 0 или !="      "
class EmptyConstraint extends ValueConstraint {
    EmptyConstraint() {super(CompareWhere.NOT_EQUALS);}
}
