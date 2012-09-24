package platform.server.logics;

import platform.server.classes.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.SystemActionProperty;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

public class SeekActionProperty extends SystemActionProperty {

    private final CalcProperty property;

    public SeekActionProperty(BaseClass baseClass, CalcProperty property) {
        super("SEEKOBJECT" + (property!=null ? "_" + property.getSID() : null ),
                ServerResourceBundle.getString("logics.property.actions.seekobject") + (property!=null ? "(" + property.getSID() + ")" : null ),
                new ValueClass[]{baseClass});
        this.property = property;
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.emitExceptionIfNotInFormSession();

        FormInstance<?> form = context.getFormInstance();
        Collection<ObjectInstance> objects;
        if (property != null)
            objects = form.instanceFactory.getInstance(form.entity.getPropertyObject(property)).mapping.values();
        else
            objects = form.getObjects();
        for (Map.Entry<ClassPropertyInterface, DataObject> key : context.getKeys().entrySet())
            if (context.getObjectInstance(key.getKey()) == null) {
                ConcreteClass keyClass = context.getSession().getCurrentClass(key.getValue());
                for (ObjectInstance object : objects)
                    if (keyClass instanceof ConcreteValueClass && object.getBaseClass().isCompatibleParent((ValueClass) keyClass))
                        form.seekObject(object, key.getValue());
            }
    }
}
