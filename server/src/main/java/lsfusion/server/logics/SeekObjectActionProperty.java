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
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class SeekObjectActionProperty extends SeekActionProperty {
    
    private final ObjectEntity object;
    private boolean last = false;

    @SuppressWarnings("UnusedDeclaration")
    public SeekObjectActionProperty(BaseLogicsModule lm, ValueClass... classes) {
        super(lm, classes);

        object = null;
    }

    public SeekObjectActionProperty(ScriptingLogicsModule lm, ObjectEntity object, boolean last) {
        super(lm, LocalizedString.concatList("Найти объект (", object.getCaption(), ")"), object.baseClass);

        this.object = object;
        this.last = last;
    }

    @Override
    protected boolean allowNulls() {
        return object != null;
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
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
