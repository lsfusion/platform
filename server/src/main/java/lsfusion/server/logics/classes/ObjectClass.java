package lsfusion.server.logics.classes;

import lsfusion.server.logics.action.ActionProperty;

public interface ObjectClass extends RemoteClass {

    ActionProperty getChangeClassAction();

    BaseClass getBaseClass();
}
