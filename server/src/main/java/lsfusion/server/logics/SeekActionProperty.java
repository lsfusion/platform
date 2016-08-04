package lsfusion.server.logics;

import lsfusion.base.SFunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ConcreteValueClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class SeekActionProperty extends ScriptingActionProperty {

    private final ObjectEntity object;
    private boolean last = false;

    @SuppressWarnings("UnusedDeclaration")
    public SeekActionProperty(BaseLogicsModule lm, ValueClass... classes) {
        super(lm, classes);

        object = null;
    }
    
    public SeekActionProperty(ScriptingLogicsModule lm, ObjectEntity object) {
        this(lm, object, false);    
    }
    
    public SeekActionProperty(ScriptingLogicsModule lm, ObjectEntity object, boolean last) {
        super(lm, "Найти объект (" + object.caption + ")", object.baseClass);

        this.object = object;
        this.last = last;
    }

    @Override
    protected boolean allowNulls() {
        return object != null;
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
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

        boolean firstObject = true;
        for (ObjectInstance object : objects) {
            if (firstObject) {
                object.groupTo.seek(last);
                firstObject = false;
            }
            form.seekObject(object, value, last);
        }
    }
}
