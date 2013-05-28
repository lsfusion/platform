package platform.server.logics;

import platform.base.SFunctionSet;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteValueClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.SystemActionProperty;
import platform.server.logics.property.actions.SystemExplicitActionProperty;

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
