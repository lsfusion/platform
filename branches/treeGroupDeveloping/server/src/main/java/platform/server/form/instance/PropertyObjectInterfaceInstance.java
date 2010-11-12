package platform.server.form.instance;

import platform.server.classes.ConcreteClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.logics.DataObject;

import java.util.Set;

public interface PropertyObjectInterfaceInstance extends OrderInstance {

    AndClassSet getClassSet(Set<GroupObjectInstance> gridGroups);

    DataObject getDataObject();

    boolean isNull();

    ConcreteClass getCurrentClass();
}
