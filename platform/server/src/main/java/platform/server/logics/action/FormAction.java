package platform.server.logics.action;

import platform.server.view.navigator.NavigatorForm;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyInterfaceImplement;
import platform.server.logics.DataObject;
import platform.server.data.where.classes.ClassWhere;
import platform.server.classes.ValueClass;
import platform.interop.form.RemoteFormInterface;

import java.util.*;

public class FormAction extends Action<FormAction.Interface> {

    NavigatorForm form;
    
    public static class Interface extends ActionInterface {

        public final ObjectNavigator object;

        private Interface(ObjectNavigator object) {
            this.object = object;
        }
    }

    private static <T extends PropertyInterface> List<Interface> getInterfaces(ObjectNavigator... objects) {
        List<Interface> interfaces = new ArrayList<Interface>();
        for(ObjectNavigator object : objects)
            interfaces.add(new Interface(object));
        return interfaces;
    }


    public FormAction(String sID, String caption, NavigatorForm form, ObjectNavigator... objects) {
        super(sID, caption, getInterfaces(objects));
        this.form = form;
    }

    public Map<Interface,ObjectNavigator> getMapInterfaces() {
        Map<Interface,ObjectNavigator> result = new HashMap<Interface, ObjectNavigator>();
        for(Interface actionInterface : interfaces)
            result.put(actionInterface,actionInterface.object);
        return result;
    }

    public ClassWhere<Interface> getClassWhere() {
        Map<Interface, ValueClass> result = new HashMap<Interface, ValueClass>();
        for(Map.Entry<Interface,ObjectNavigator> mapInterface : getMapInterfaces().entrySet())
            result.put(mapInterface.getKey(),mapInterface.getValue().baseClass);
        return new ClassWhere<Interface>(result, true);
    }

    public RemoteFormInterface execute(Map<Interface, DataObject> objects) {
        return null;
    }
}
