package lsfusion.server.logics.form.interactive.action.seek;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ConcreteValueClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class SeekInternalAction extends InternalAction {

    public SeekInternalAction(BaseLogicsModule lm, ValueClass... classes) {
        super(lm, classes);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type.isChange())
            return true;
        return super.hasFlow(type);
    }

    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance form = context.getFormInstance(false, true);

        ImSet<ObjectInstance> objects;
        ObjectValue value;

        if (context.getSingleObjectInstance() == null) {
            DataObject dataValue = context.getSingleDataKeyValue();
            final ConcreteClass keyClass = context.getSession().getCurrentClass(dataValue);
            objects = form.getObjects().filterFn(object -> keyClass instanceof ConcreteValueClass && object.getBaseClass().isCompatibleParent((ValueClass) keyClass));
            value = dataValue;
        } else {
            ServerLoggers.assertLog(false, "SCRIPTING SEEK IS ALWAYS WITHOUT OBJECT");
            objects = SetFact.EMPTY();
            value = NullValue.instance;
        }

        for(ImSet<ObjectInstance> groupedObjects : objects.group(key -> key.groupTo).valueIt())
            form.seekObjects(null, groupedObjects.toMap(value), UpdateType.FIRST);
    }
}
