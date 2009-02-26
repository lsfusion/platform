package platform.server.logics.constraints;

import platform.interop.Compare;

// != 0 или !="      "
class EmptyConstraint extends ValueConstraint {
    EmptyConstraint() {super(Compare.NOT_EQUALS);}
}
