package platform.server.classes;

import platform.server.logics.property.ActionProperty;

public interface ObjectClass extends RemoteClass {

    public ActionProperty getChangeClassAction();

    public BaseClass getBaseClass();
}
