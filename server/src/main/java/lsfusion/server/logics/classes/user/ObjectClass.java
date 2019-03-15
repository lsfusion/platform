package lsfusion.server.logics.classes.user;

import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.classes.AClass;

public interface ObjectClass extends AClass {

    Action getChangeClassAction();

    BaseClass getBaseClass();
}
