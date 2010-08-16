package platform.server.logics;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.interop.action.FormClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.form.instance.remote.RemoteForm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public class FormActionProperty extends ActionProperty {

    public final FormEntity form;
    public final Map<ObjectEntity, ClassPropertyInterface> mapObjects;

    public static ValueClass[] getValueClasses(ObjectEntity[] objects) {
        ValueClass[] valueClasses = new ValueClass[objects.length];
        for(int i=0;i<objects.length;i++)
            valueClasses[i] = objects[i].baseClass;
        return valueClasses;
    }

    public FormActionProperty(String sID, String caption, FormEntity form, ObjectEntity[] objects) {
        super(sID, caption, getValueClasses(objects));

        int i=0; // такой же дебилизм и в SessionDataProperty 
        mapObjects = new HashMap<ObjectEntity, ClassPropertyInterface>();
        for(ClassPropertyInterface propertyInterface : interfaces)
            mapObjects.put(objects[i++],propertyInterface);
        this.form = form;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapExecuteObjects) {
        actions.add(new FormClientAction(form.isPrintForm, executeForm.createForm(form, BaseUtils.join(mapObjects,keys)))); 
    }
}
