package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ConnectionService;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.DBFWriter;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.JDBFException;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.JDBField;
import lsfusion.server.logics.form.stat.struct.plain.JDBCTable;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.substring;

public class ExternalDBFAction extends CallAction {
    private PropertyInterface connectionString;

    private String charset;

    public ExternalDBFAction(ImList<Type> params, String charset, ImList<LP> targetPropList) {
        super(1, params, targetPropList);

        this.connectionString = getOrderInterfaces().get(0);
        this.charset = charset == null ? "UTF-8" : charset;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        writeDBF(context, replaceParams(context, getTransformedText(context, connectionString)));
        return FlowResult.FINISH;
    }

    private void writeDBF(ExecutionContext<PropertyInterface> context, String connectionString) {
        try {
            PropertyInterface paramInterface = paramInterfaces.single();
            ObjectValue paramValue = context.getKeyValue(paramInterface);
            if (paramValue instanceof DataObject) {
                DataObject paramObject = (DataObject) paramValue;
                DataClass paramClass = (DataClass) getFileClass(paramObject, paramTypes.get(paramInterface));
                if (paramClass instanceof FileClass) {
                    JDBCTable jdbcTable = readTableFile(paramObject, paramClass);
                    if (jdbcTable != null) {
                        File file = new File(connectionString);

                        DBFWriter dbfFile = null;
                        ConnectionService connectionService = context.getConnectionService();
                        if (connectionService != null)
                            dbfFile = connectionService.getDBFFile(connectionString);
                        else if (connectionString.isEmpty())
                            throw new UnsupportedOperationException("Empty connectionString is supported only inside of NEWCONNECTION operator");

                        if (dbfFile == null) {
                            // when appending, the fields are read from the existing file layout and the values are matched by name
                            boolean append = file.exists() && file.length() > 0;
                            dbfFile = append ? new DBFWriter(file.getAbsolutePath(), charset)
                                             : new DBFWriter(file.getAbsolutePath(), getFields(jdbcTable), charset);
                            if (connectionService != null)
                                connectionService.putDBFFile(connectionString, dbfFile);
                        }

                        try {
                            JDBField[] fields = dbfFile.getFields();
                            for (ImMap<String, Object> row : jdbcTable.set) {
                                Object[] record = new Object[fields.length];
                                for (int i = 0; i < fields.length; i++)
                                    record[i] = row.get(fields[i].getName()); // the value conversion / checks are done by JDBField.format (like in EXPORT DBF FROM)
                                dbfFile.addRecord(record);
                            }
                        } finally {
                            if (connectionService == null)
                                dbfFile.close();
                        }
                    }
                }
            }
        } catch (IOException | JDBFException e) {
            throw Throwables.propagate(e);
        }
    }

    private JDBField[] getFields(JDBCTable jdbcTable) throws JDBFException {
        List<JDBField> dbfFields = new ArrayList<>();

        for (String field : formatFieldNames(jdbcTable.fields.toJavaList())) {
            Type type = jdbcTable.fieldTypes.get(field);
            // the same field mapping as in EXPORT DBF FROM (see ExportDBFWriter)
            dbfFields.add(type != null ? type.formatDBF(field) : JDBField.createField(field, 'C', 253, 0));
        }

        return dbfFields.toArray(new JDBField[0]);
    }

    private List<String> formatFieldNames(List<String> fieldNames) {
        int maxLength = 10;
        List<String> usedFieldNames = new ArrayList<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = substring(fieldNames.get(i), maxLength);
            if (!usedFieldNames.contains(fieldName))
                usedFieldNames.add(fieldName);
            else
                throw new RuntimeException("Export Error: duplicate field '" + fieldName + "'. Fields should be unique!");

        }
        return usedFieldNames;
    }
}
