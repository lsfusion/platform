package lsfusion.server.logics;

import lsfusion.base.SFunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ConcreteValueClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

import java.sql.SQLException;

public class SeekActionProperty extends SystemExplicitActionProperty {

    private final ObjectEntity object;

    public SeekActionProperty(BaseClass baseClass) {
        super("seek", ServerResourceBundle.getString("logics.property.actions.seekobject"), new ValueClass[]{baseClass});

        object = null;
    }

    public SeekActionProperty(ObjectEntity object) {
        super("seek_" + object.getSID(), ServerResourceBundle.getString("logics.property.actions.seekobject") + " (" + object.caption + ")", new ValueClass[]{object.baseClass});

        this.object = object;
    }

    @Override
    protected boolean allowNulls() {
        return object != null;
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.emitExceptionIfNotInFormSession();

        FormInstance<?> form = context.getFormInstance();

        ImSet<ObjectInstance> objects;
        ObjectValue value;

        if(object != null) {
            objects = SetFact.singleton(form.instanceFactory.getInstance(object));
            value = context.getSingleKeyValue();
        } else {
            if (context.getSingleObjectInstance() == null) {
                DataObject dataValue = context.getSingleDataKeyValue();
                final ConcreteClass keyClass = context.getSession().getCurrentClass(dataValue);
                objects = form.getObjects().filterFn(new SFunctionSet<ObjectInstance>() {
                    public boolean contains(ObjectInstance object) {
                        return keyClass instanceof ConcreteValueClass && object.getBaseClass().isCompatibleParent((ValueClass) keyClass);
                    }});
                value = dataValue;
            } else {
                objects = SetFact.EMPTY();
                value = NullValue.instance;
            }
        }

        for (ObjectInstance object : objects)
            form.seekObject(object, value);
    }
}
