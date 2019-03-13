package lsfusion.server.physics.dev.migration;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.*;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.physics.exec.table.ImplementTable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
