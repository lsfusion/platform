package lsfusion.server.logics.service;

import lsfusion.base.col.lru.ALRUMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

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
