package platform.server.form.instance;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.classes.ConcreteClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.logics.DataObject;

public interface PropertyObjectInterfaceInstance extends OrderInstance {

    AndClassSet getClassSet(ImSet<GroupObjectInstance> gridGroups);

    DataObject getDataObject();

    boolean isNull();

    ConcreteClass getCurrentClass();
}
