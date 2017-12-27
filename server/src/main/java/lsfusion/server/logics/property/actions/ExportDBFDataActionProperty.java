package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import com.hexiong.jdbf.DBFWriter;
import com.hexiong.jdbf.JDBFException;
import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.*;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.exporting.dbf.OverJDBField;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.substring;

public class ExportDBFDataActionProperty<I extends PropertyInterface> extends ExportDataActionProperty<I> {

    private String charset;

    public ExportDBFDataActionProperty(LocalizedString caption, String extension,
                                       ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces,
                                       ImOrderSet<String> fields, ImMap<String, CalcPropertyInterfaceImplement<I>> exprs, CalcPropertyInterfaceImplement<I> where, LCP targetProp,
                                       String charset) {
        super(caption, extension, innerInterfaces, mapInterfaces, fields, exprs, where, targetProp);

        this.charset = charset == null ? "UTF-8" : charset;
    }

    @Override
    protected byte[] getFile(final Query<I, String> query, ImList<ImMap<String, Object>> rows, Type.Getter<String> fieldTypes) throws IOException {
        DBFWriter writer = null;
        File file = null;
        try {
            file = File.createTempFile("export", ".dbf");
            OverJDBField[] jdbfFields = getFields(fieldTypes);

            writer = new DBFWriter(file.getAbsolutePath(), jdbfFields, charset);
            for (ImMap<String, Object> row : rows) {
                writer.addRecord(row.values().toArray(new Object[row.size()]));
            }
            return IOUtils.getFileBytes(file);

        } catch (JDBFException e) {
            throw Throwables.propagate(e);
        } finally {
            closeWriter(writer);
            if (file != null && !file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    private OverJDBField[] getFields(Type.Getter<String> fieldTypes) throws JDBFException {
        List<OverJDBField> dbfFields = new ArrayList<>();

        for (String field : formatFieldNames(fields.toJavaList())) {

            Type type = fieldTypes.getType(field);
            if (type == DoubleClass.instance)
                dbfFields.add(new OverJDBField(field, 'F', 10, 3));
            else if (type instanceof IntegerClass)
                dbfFields.add(new OverJDBField(field, 'N', 253, ((IntegerClass) type).getPrecision()));
            else if (type instanceof NumericClass)
                dbfFields.add(new OverJDBField(field, 'N', 253, ((NumericClass) type).getPrecision()));
            else if (type instanceof DateClass || type instanceof DateTimeClass) {
                dbfFields.add(new OverJDBField(field, 'D', 8, 0));
            } else if (type instanceof LogicalClass)
                dbfFields.add(new OverJDBField(field, 'L', 1, 0));
            else if (type instanceof StringClass)
                dbfFields.add(new OverJDBField(field, 'C', ((StringClass) type).length.value, 0));
            else if (type instanceof TimeClass)
                dbfFields.add(new OverJDBField(field, 'C', 8, 0));
            else
                dbfFields.add(new OverJDBField(field, 'C', 253, 0));
        }

        return dbfFields.toArray(new OverJDBField[dbfFields.size()]);
    }

    private List<String> formatFieldNames(List<String> fieldNames) {
        int maxLength = 10;
        List<String> usedFieldNames = new ArrayList<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = substring(fieldNames.get(i), maxLength);
            if(!usedFieldNames.contains(fieldName))
                usedFieldNames.add(fieldName);
            else
                throw new RuntimeException("Export Error: duplicate field '" + fieldName + "'. Fields should be unique!");

        }
        return usedFieldNames;
    }

    private void closeWriter(DBFWriter writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (JDBFException e) {
                e.printStackTrace();
            }
        }
    }
}
