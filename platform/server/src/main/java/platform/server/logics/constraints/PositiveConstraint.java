package platform.server.logics.constraints;

import platform.interop.Compare;

// < 0
class PositiveConstraint extends ValueConstraint {
    PositiveConstraint() {super(Compare.LESS);}
}
