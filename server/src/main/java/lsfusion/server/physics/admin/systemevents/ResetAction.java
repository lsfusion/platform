package lsfusion.server.physics.admin.systemevents;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResetAction extends SystemExplicitAction {
    private final Property sourceProperty;

    public ResetAction(LocalizedString caption, Property sourceProperty) {
        super(caption, new LP(sourceProperty).getInterfaceClasses(ClassType.resetPolicy));
        this.sourceProperty = sourceProperty;       
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
    
    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        List<DataObject> objectValues  = new ArrayList<>();
        for(ClassPropertyInterface entry : interfaces)
            objectValues.add((DataObject) context.getKeyValue(entry));

        new LP(sourceProperty).change(NullValue.instance, context, objectValues.toArray(new DataObject[0]));
    }
}