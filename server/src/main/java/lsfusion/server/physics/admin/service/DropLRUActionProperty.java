package lsfusion.server.physics.admin.service;

import lsfusion.base.col.lru.ALRUMap;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.Iterator;

public class DropLRUActionProperty extends ScriptingActionProperty {

    @Override
    protected boolean allowNulls() {
        return true;
    }

    private final ClassPropertyInterface percentInterface;
    private final ClassPropertyInterface randomInterface;

    public DropLRUActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        percentInterface = i.next();
        randomInterface = i.next();

    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        final ObjectValue keyValue = context.getKeyValue(percentInterface);
        if(keyValue instanceof DataObject) {
            final double percent = ((Double) ((DataObject) keyValue).object) / 100.0;
            if (context.getKeyValue(randomInterface).isNull()) {
                ALRUMap.forceRemoveAllLRU(percent);
            } else {
                ALRUMap.forceRandomRemoveAllLRU(percent);
            }
        }
    }
}
