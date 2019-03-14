package lsfusion.server.logics.form.interactive.action.seek;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

import java.sql.SQLException;
import java.util.List;

public class SeekGroupObjectActionProperty extends SeekActionProperty {

    private final GroupObjectEntity groupObject;
    private final List<ObjectEntity> objects;
    private UpdateType type;

    public SeekGroupObjectActionProperty(GroupObjectEntity groupObject, List<ObjectEntity> objects, UpdateType type, ValueClass... classes) {
        super(LocalizedString.create("Найти объект (" + groupObject.getSID() + ")", false), classes);

        this.groupObject = groupObject;
        this.objects = objects;
        this.type = type;
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        if (objects == null || objects.isEmpty()) {
            groupObject.getInstance(form.instanceFactory).seek(type);
        } else {
            for (int i = 0; i < objects.size(); ++i) {
                ObjectInstance instance = form.instanceFactory.getInstance(objects.get(i));
                if (i == 0) {
                    instance.groupTo.seek(type);
                }
                form.seekObject(instance, context.getKeyValue(getOrderInterfaces().get(i)), type);
            }
        }
    }
}