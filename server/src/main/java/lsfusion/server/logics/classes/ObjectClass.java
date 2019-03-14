package lsfusion.server.logics.classes;

import lsfusion.server.logics.action.Action;

public interface ObjectClass extends RemoteClass {

    Action getChangeClassAction();

    BaseClass getBaseClass();
}
