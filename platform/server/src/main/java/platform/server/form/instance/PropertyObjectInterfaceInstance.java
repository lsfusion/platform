package platform.server.form.instance;

import platform.server.classes.ConcreteClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.logics.DataObject;

public interface PropertyObjectInterfaceInstance extends OrderInstance {

    AndClassSet getClassSet(GroupObjectInstance classGroup);

    DataObject getDataObject();

    boolean isNull();

    ConcreteClass getCurrentClass();
}
