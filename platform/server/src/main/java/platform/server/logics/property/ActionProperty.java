package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.classes.ConcreteValueClass;
import platform.server.classes.ActionClass;
import platform.server.logics.DataObject;
import platform.interop.form.RemoteFormInterface;

import java.util.Map;

public class ActionProperty extends ClassProperty {

    public ActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes, ActionClass.instance, true);
    }

    public RemoteFormInterface execute(Map<ClassPropertyInterface, DataObject> objects) {
        return null;
    }
}
