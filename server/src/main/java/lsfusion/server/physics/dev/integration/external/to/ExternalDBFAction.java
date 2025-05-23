package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.file.FileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ConnectionService;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.data.file.TableClass;
import lsfusion.server.logics.classes.data.integral.DoubleClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.logics.classes.data.time.TimeClass;
import lsfusion.server.logics.form.stat.struct.plain.JDBCTable;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import org.xBaseJ.DBF;
import org.xBaseJ.fields.CharField;
import org.xBaseJ.fields.DateField;
import org.xBaseJ.fields.Field;
import org.xBaseJ.fields.LogicalField;
import org.xBaseJ.xBaseJException;

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
                    FileData fileData = readFile(paramObject, paramClass, null);
                    String extension = fileData.getExtension();
                    if (extension.equals(TableClass.extension)) { // значит таблица
                        JDBCTable jdbcTable = JDBCTable.deserializeJDBC(fileData.getRawFile());

                        Field[] fields = getFields(jdbcTable);
                        File file = new File(connectionString);
                        boolean append = file.exists();

                        DBF dbfFile = null;
                        ConnectionService connectionService = context.getConnectionService();
                        if (connectionService != null)
                            dbfFile = connectionService.getDBFFile(connectionString);
                        else if (connectionString.isEmpty())
                            throw new UnsupportedOperationException("Empty connectionString is supported only inside of NEWCONNECTION operator");

                        if (dbfFile == null) {
                            if (append) {
                                dbfFile = new DBF(file.getAbsolutePath(), charset);
                            } else {
                                dbfFile = new DBF(file.getAbsolutePath(), DBF.DBASEIV, true, charset);
                                dbfFile.addField(fields);
                            }
                            if (connectionService != null)
                                connectionService.putDBFFile(connectionString, dbfFile);
                        }

                        try {
                            for (ImMap<String, Object> row : jdbcTable.set) {
                                for (Field field : fields) {
                                    putField(dbfFile, field, String.valueOf(row.get(field.getName())), append);
                                }
                                dbfFile.write();
                            }
                        } finally {
                            if (connectionService == null)
                                dbfFile.close();
                        }
                    }
                }
            }
        } catch (IOException | xBaseJException e) {
            throw Throwables.propagate(e);
        }
    }

    private void putField(DBF dbfFile, Field field, String value, boolean append) throws xBaseJException {
        if(append)
            dbfFile.getField(field.getName()).put(value == null ? "null" : value);
        else
            field.put(value == null ? "null" : value);
    }

    private Field[] getFields(JDBCTable jdbcTable) throws IOException, xBaseJException {
        List<Field> dbfFields = new ArrayList<>();

        //TODO: форматировать значение в строку
        for (String field : formatFieldNames(jdbcTable.fields.toJavaList())) {
            Type type = jdbcTable.fieldTypes.get(field);
            if (type == DoubleClass.instance)
                dbfFields.add(new NumField2(field, 10, 3));
            else if (type instanceof IntegerClass)
                dbfFields.add(new NumField2(field, 10, 0));
            else if (type instanceof NumericClass)
                dbfFields.add(new NumField2(field, 10, ((NumericClass) type).getScale()));
            else if (type instanceof ObjectType)
                dbfFields.add(new NumField2(field, 10, 0));
            else if (type instanceof DateClass || type instanceof TimeClass || type instanceof DateTimeClass) {
                dbfFields.add(new DateField(field));
            } else if (type instanceof LogicalClass)
                dbfFields.add(new LogicalField(field));
            else if (type instanceof StringClass) {
                int length = ((StringClass) type).length.value;
                dbfFields.add(new CharField(field, length < 0 || length > 253 ? 253 : length));
            } else
                dbfFields.add(new CharField(field, 253));
        }

        return dbfFields.toArray(new Field[0]);
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