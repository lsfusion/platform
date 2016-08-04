package lsfusion.server.logics;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.List;

public class SeekGroupObjectActionProperty extends ScriptingActionProperty {

    private final GroupObjectEntity groupObject;
    private final List<ObjectEntity> objects;
    private boolean last;

    public SeekGroupObjectActionProperty(ScriptingLogicsModule lm, GroupObjectEntity groupObject, List<ObjectEntity> objects, boolean last, ValueClass... classes) {
        super(lm, "Найти объект (" + groupObject.getSID() + ")", classes);

        this.groupObject = groupObject;
        this.objects = objects;
        this.last = last;
    }

    @Override
    protected boolean allowNulls() {
        return groupObject != null;
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.emitExceptionIfNotInFormSession();

        if (groupObject != null) {
            if (objects == null || objects.isEmpty()) {
                groupObject.getInstance(context.getFormInstance().instanceFactory).addSeek(last);
            } else {
                FormInstance<?> form = context.getFormInstance();
                for (int i = 0; i < objects.size(); ++i) {
                    ObjectInstance instance = form.instanceFactory.getInstance(objects.get(i));
                    if (i == 0) {
                        instance.groupTo.seek(last);
                    }
                    form.seekObject(instance, context.getKeyValue(getOrderInterfaces().get(i)), last);
                }
            }
        }
    }
}