package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.sets.AndClassSet;
import lsfusion.server.logics.form.interactive.instance.order.OrderInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;

public interface PropertyObjectInterfaceInstance extends OrderInstance {

    AndClassSet getClassSet(ImSet<GroupObjectInstance> gridGroups);

    DataObject getDataObject();
    ObjectValue getObjectValue();

    boolean isNull();

    ConcreteClass getCurrentClass();
}
