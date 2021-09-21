package lsfusion.server.logics.form.interactive.action.seek;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class SeekGroupObjectAction extends SeekAction {

    private final GroupObjectEntity groupObject;
    private final ImOrderSet<ObjectEntity> objects;
    private UpdateType type;

    public SeekGroupObjectAction(GroupObjectEntity groupObject, ImOrderSet<ObjectEntity> objects, UpdateType type, ValueClass... classes) {
        super(LocalizedString.NONAME, classes);

        this.groupObject = groupObject;
        this.objects = objects;
        this.type = type;
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.seekObjects(groupObject.getInstance(form.instanceFactory), objects.mapOrderSetValues(form.instanceFactory::getInstance).mapOrderValues((int i) -> context.getKeyValue(getOrderInterfaces().get(i))), type);
    }
}