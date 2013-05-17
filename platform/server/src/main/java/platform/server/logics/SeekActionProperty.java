package platform.server.logics;

import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteValueClass;
import platform.server.classes.ValueClass;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.SystemActionProperty;

import java.sql.SQLException;

public class SeekActionProperty extends SystemActionProperty {

    public SeekActionProperty(BaseClass baseClass) {
        super("seek", ServerResourceBundle.getString("logics.property.actions.seekobject"), new ValueClass[]{baseClass});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.emitExceptionIfNotInFormSession();

        FormInstance<?> form = context.getFormInstance();
        ImCol<ObjectInstance> objects = form.getObjects();
        ImMap<ClassPropertyInterface,DataObject> keys = context.getKeys();
        for (int i=0,size=keys.size();i<size;i++)
            if (context.getObjectInstance(keys.getKey(i)) == null) {
                DataObject value = keys.getValue(i);
                ConcreteClass keyClass = context.getSession().getCurrentClass(value);
                for (ObjectInstance object : objects)
                    if (keyClass instanceof ConcreteValueClass && object.getBaseClass().isCompatibleParent((ValueClass) keyClass))
                        form.seekObject(object, value);
            }
    }
}
