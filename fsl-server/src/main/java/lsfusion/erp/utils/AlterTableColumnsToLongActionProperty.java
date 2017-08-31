package lsfusion.erp.utils;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.base.BaseUtils.serviceLogger;

public class AlterTableColumnsToLongActionProperty extends ScriptingActionProperty {

    private final ClassPropertyInterface tableInterface;
    

    public AlterTableColumnsToLongActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {

        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        tableInterface = i.next();
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try (DataSession session = context.createSession()) {
            session.sql.pushNoReadOnly();
            try {

                DataObject tableObject = context.getDataKeyValue(tableInterface);
                String tableName = (String) context.getBL().reflectionLM.sidTable.read(context, tableObject);

                ImSet<ImplementTable> tables = context.getBL().LM.tableFactory.getImplementTables();
                for (ImplementTable dataTable : tables) {
                    if(tableName.equals(dataTable.getName())) {
                        serviceLogger.info(String.format("Alter Table %s: %s keys, %s properties", String.valueOf(dataTable), dataTable.keys.size(), dataTable.properties.size()));
                        for (KeyField keyField : dataTable.keys) {
                            if (keyField.type instanceof ObjectType) {
                                session.sql.executeDDL(String.format("ALTER TABLE %s ALTER COLUMN %s TYPE BIGINT", String.valueOf(dataTable), keyField.getName()));
                            }
                        }
                        for (PropertyField propertyField : dataTable.properties) {
                            if (propertyField.type instanceof ObjectType) {
                                session.sql.executeDDL(String.format("ALTER TABLE %s ALTER COLUMN %s TYPE BIGINT", String.valueOf(dataTable), propertyField.getName()));
                            }
                        }
                    }
                }
                session.apply(context);
            } finally {
                session.sql.popNoReadOnly();
            }
        }

    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}
