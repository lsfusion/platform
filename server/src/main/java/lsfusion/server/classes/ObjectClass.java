package lsfusion.server.classes;

import lsfusion.server.logics.property.ActionProperty;

public interface ObjectClass extends RemoteClass {

    public ActionProperty getChangeClassAction();

    public BaseClass getBaseClass();
}
