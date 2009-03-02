package platform.server.logics.constraints;

import platform.interop.Compare;

// <= 0 или <= ''
class NotEmptyConstraint extends ValueConstraint {
    NotEmptyConstraint() {super(Compare.LESS_EQUALS);}
}
