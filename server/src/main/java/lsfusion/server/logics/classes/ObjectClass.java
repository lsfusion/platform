package lsfusion.server.logics.classes;

import lsfusion.server.logics.property.ActionProperty;

public interface ObjectClass extends RemoteClass {

    ActionProperty getChangeClassAction();

    BaseClass getBaseClass();
}
