package lsfusion.server.logics;

import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.ServerLoggers;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ConcreteValueClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.language.ScriptingActionProperty;

import java.sql.SQLException;

public class SeekScriptingActionProperty extends ScriptingActionProperty {

    public SeekScriptingActionProperty(BaseLogicsModule lm, ValueClass... classes) {
        super(lm, classes);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type.isChange())
            return true;
        return super.hasFlow(type);
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance form = context.getFormInstance(false, true);

        ImSet<ObjectInstance> objects;
        ObjectValue value;

        if (context.getSingleObjectInstance() == null) {
            DataObject dataValue = context.getSingleDataKeyValue();
            final ConcreteClass keyClass = context.getSession().getCurrentClass(dataValue);
            objects = form.getObjects().filterFn(new SFunctionSet<ObjectInstance>() {
                public boolean contains(ObjectInstance object) {
                    return keyClass instanceof ConcreteValueClass && object.getBaseClass().isCompatibleParent((ValueClass) keyClass);
                }});
            value = dataValue;
        } else {
            ServerLoggers.assertLog(false, "SCRIPTING SEEK IS ALWAYS WITHOUT OBJECT");
            objects = SetFact.EMPTY();
            value = NullValue.instance;
        }

        boolean firstObject = true;
        for (ObjectInstance object : objects) {
            if (firstObject) {
                object.groupTo.seek(UpdateType.FIRST);
                firstObject = false;
            }
            form.seekObject(object, value, UpdateType.FIRST);
        }
    }
}
