package platform.server.view.form;

import platform.server.classes.sets.AndClassSet;
import platform.server.classes.ConcreteClass;
import platform.server.logics.DataObject;

public interface PropertyObjectInterface extends OrderView {

    AndClassSet getClassSet(GroupObjectImplement classGroup);

    DataObject getDataObject();

    ConcreteClass getCurrentClass();
}
