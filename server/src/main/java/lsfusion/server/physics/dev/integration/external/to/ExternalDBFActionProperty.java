package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.*;
import lsfusion.server.data.JDBCTable;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.classes.*;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.action.flow.FlowResult;
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

public class ExternalDBFActionProperty extends ExternalActionProperty {
    private PropertyInterface connectionString;

    private String charset;

    public ExternalDBFActionProperty(ImList<Type> params, String charset, ImList<LCP> targetPropList) {
        super(1, params, targetPropList);

        this.connectionString = getOrderInterfaces().get(0);
        this.charset = charset == null ? "UTF-8" : charset;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        writeDBF(context, replaceParams(context, getTransformedText(context, connectionString)));
        return FlowResult.FINISH;
    }

    private void writeDBF(ExecutionContext<PropertyInterface> context, String connectionString) throws SQLException, SQLHandledException {
        try {
            Object fileData = context.getKeyObject(paramInterfaces.single());
            if (fileData instanceof FileData) {
                String extension = ((FileData) fileData).getExtension();
                if (extension.equals("jdbc")) { // значит таблица
                    JDBCTable jdbcTable = JDBCTable.deserializeJDBC(((FileData) fileData).getRawFile());

                    Field[] fields = getFields(jdbcTable);
                    File file = new File(connectionString);
                    boolean append = file.exists();
                    DBF dbfFile = null;
                    try {
                        if (append) {
                            dbfFile = new DBF(file.getAbsolutePath(), charset);
                        } else {
                            dbfFile = new DBF(file.getAbsolutePath(), DBF.DBASEIV, true, charset);
                            dbfFile.addField(fields);
                        }
                        for (ImMap<String, Object> row : jdbcTable.set) {
                            for (int i = 0; i < fields.length; i++) {
                                putField(dbfFile, fields[i], String.valueOf(row.values().get(i)), append);
                            }
                            dbfFile.write();
                        }
                    } finally {
                        if (dbfFile != null)
                            dbfFile.close();
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
                dbfFields.add(new NumField2(field, 10, ((NumericClass) type).getPrecision()));
            else if (type instanceof ObjectType)
                dbfFields.add(new NumField2(field, 10, 0));
            else if (type instanceof DateClass || type instanceof TimeClass || type instanceof DateTimeClass) {
                dbfFields.add(new DateField(field));
            } else if (type instanceof LogicalClass)
                dbfFields.add(new LogicalField(field));
            else if (type instanceof StringClass)
                dbfFields.add(new CharField(field, ((StringClass) type).length.value));
            else
                dbfFields.add(new CharField(field, 253));
        }

        return dbfFields.toArray(new Field[dbfFields.size()]);
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