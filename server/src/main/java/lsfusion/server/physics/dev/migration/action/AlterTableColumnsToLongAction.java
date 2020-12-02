package lsfusion.server.physics.dev.migration.action;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.table.Field;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.table.ImplementTable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static lsfusion.base.BaseUtils.serviceLogger;

public class AlterTableColumnsToLongAction extends InternalAction {

    private final ClassPropertyInterface tableInterface;


    public AlterTableColumnsToLongAction(ScriptingLogicsModule LM, ValueClass... classes) {

        super(LM, classes);
        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        tableInterface = i.next();
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        SQLSession sql = context.getSession().sql;
        sql.pushNoReadOnly();
        try {
            DataObject tableObject = context.getDataKeyValue(tableInterface);
            String tableName = (String) context.getBL().reflectionLM.sidTable.read(context, tableObject);

            ImSet<ImplementTable> tables = context.getBL().LM.tableFactory.getImplementTables();
            for (ImplementTable dataTable : tables) {
                if (tableName.equals(dataTable.getName())) {
                    List<Field> fieldList = new ArrayList<>();
                    for (KeyField keyField : dataTable.keys) {
                        if (keyField.type instanceof ObjectType) {
                            fieldList.add(keyField);
                        }
                    }
                    for (PropertyField propertyField : dataTable.properties) {
                        if (propertyField.type instanceof ObjectType) {
                            fieldList.add(propertyField);
                        }
                    }

                    StringBuilder ddl = new StringBuilder();
                    for (Field field : fieldList) {
                        ddl.append(String.format(((ddl.length() == 0) ? "" : ",") + " ALTER COLUMN %s TYPE BIGINT", field.getName()));
                    }
                    serviceLogger.info(String.format("Alter Table %s: %s field(s)", String.valueOf(dataTable), fieldList.size()));
                    if (ddl.length() > 0) {
                        sql.executeDDL("ALTER TABLE " + dataTable + " " + ddl);
                        sql.executeDDL("ANALYZE " + dataTable);
                    }
                }
            }
        } finally {
            sql.popNoReadOnly();
        }
    }
}
