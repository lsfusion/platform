package platform.server.view.form;

import platform.server.data.classes.where.AndClassSet;
import platform.server.data.classes.ConcreteClass;
import platform.server.logics.DataObject;
import platform.server.view.form.filter.CompareValue;

public interface PropertyObjectInterface extends OrderView {

    AndClassSet getClassSet(GroupObjectImplement classGroup);

    ConcreteClass getObjectClass();
    DataObject getDataObject();
}
