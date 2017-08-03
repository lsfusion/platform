package lsfusion.server.form.instance;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

public interface PropertyObjectInterfaceInstance extends OrderInstance {

    AndClassSet getClassSet(ImSet<GroupObjectInstance> gridGroups);

    DataObject getDataObject();
    ObjectValue getObjectValue();

    boolean isNull();

    ConcreteClass getCurrentClass();
}
