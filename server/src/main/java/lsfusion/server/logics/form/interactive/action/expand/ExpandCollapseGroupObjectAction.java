package lsfusion.server.logics.form.interactive.action.expand;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.action.seek.SeekAction;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.List;

public class ExpandCollapseGroupObjectAction extends SeekAction {

    private final GroupObjectEntity groupObject;
    private final List<ObjectEntity> objects;
    private ExpandCollapseType type;
    private boolean expand;

    public ExpandCollapseGroupObjectAction(GroupObjectEntity groupObject, List<ObjectEntity> objects, ExpandCollapseType type, boolean expand, ValueClass... classes) {
        super(LocalizedString.NONAME, classes);

        this.groupObject = groupObject;
        this.objects = objects;
        this.type = type;
        this.expand = expand;
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        FormInstance formInstance = context.getFormInstance(false, true);
        GroupObjectInstance groupObjectInstance = formInstance.instanceFactory.getExInstance(groupObject);

        ImMap<ObjectInstance, DataObject> value;
        if(objects.isEmpty()) {
            value = groupObjectInstance.getGroupObjectValue();
        } else {
            value = MapFact.EMPTY();
            for (int i = 0; i < objects.size(); ++i) {
                ObjectInstance instance = form.instanceFactory.getExInstance(objects.get(i));
                ObjectValue objectValue = context.getKeyValue(getOrderInterfaces().get(i));
                if(objectValue instanceof DataObject) {
                    value = value.addExcl(instance, (DataObject) objectValue);
                } else {
                    return; // do not expand/collapse if object not found
                }
            }
            // a row of a tree's group is keyed by its objects and those of the groups above it
            ImSet<ObjectInstance> missing = GroupObjectInstance.getObjects(groupObjectInstance.getUpTreeGroups()).remove(value.keys());
            if (!missing.isEmpty() && type != ExpandCollapseType.ALLTOP)
                throw new RuntimeException("cannot EXPAND / COLLAPSE a row of '" + groupObject.getSID() + "' with no"
                        + " OBJECTS for " + missing.toString(ObjectInstance::getSID, ", ") + ": a row of a tree's group is named"
                        + " by its objects and those of the groups above it");
        }

        switch (type) {
            case UP:
                groupObjectInstance.expandCollapseUp(form, value, expand);
                break;
            case DOWN:
                groupObjectInstance.expandCollapseDown(form, value, expand);
                break;
            case ALL: // with no OBJECTS, ALL takes the current row, and every row of the top while there is none
                groupObjectInstance.expandCollapseAll(form, objects.isEmpty() ? null : value, true, expand);
                break;
            case ALLTOP:
                groupObjectInstance.expandCollapseAll(form, false, expand);
                break;
        }
    }
}