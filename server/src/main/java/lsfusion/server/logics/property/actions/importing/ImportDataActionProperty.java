package lsfusion.server.logics.property.actions.importing;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.classes.FileClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.ImportSourceFormat;
import lsfusion.server.logics.property.actions.importing.csv.ImportCSVDataActionProperty;
import lsfusion.server.logics.property.actions.importing.dbf.ImportDBFDataActionProperty;
import lsfusion.server.logics.property.actions.importing.sql.ImportSQLDataActionProperty;
import lsfusion.server.logics.property.actions.importing.xls.ImportXLSDataActionProperty;
import lsfusion.server.logics.property.actions.importing.xlsx.ImportXLSXDataActionProperty;
import lsfusion.server.logics.property.actions.importing.xml.ImportXMLDataActionProperty;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.jdom.JDOMException;
import org.xBaseJ.xBaseJException;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ImportDataActionProperty extends ScriptingActionProperty {


    protected final List<String> ids;
    protected final List<LCP> properties;

    public static ImportDataActionProperty createProperty(ValueClass valueClass, ImportSourceFormat format, ScriptingLogicsModule LM, 
                                                          List<String> ids, List<LCP> properties, String separator, boolean noHeader) {
        if (format == ImportSourceFormat.XLS) {
            return new ImportXLSDataActionProperty(valueClass, LM, ids, properties);
        } else if (format == ImportSourceFormat.XLSX) {
            return new ImportXLSXDataActionProperty(valueClass, LM, ids, properties);
        } else if (format == ImportSourceFormat.DBF) {
            return new ImportDBFDataActionProperty(valueClass, LM, ids, properties);
        } else if (format == ImportSourceFormat.CSV) {
            return new ImportCSVDataActionProperty(valueClass, LM, ids, properties, separator, noHeader);
        } else if (format == ImportSourceFormat.XML) {
            return new ImportXMLDataActionProperty(valueClass, LM, ids, properties);
        } else if (format == ImportSourceFormat.SQL) {
            return new ImportSQLDataActionProperty(valueClass, LM, ids, properties);
        }
        return null;
    }

    public ImportDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(LM, valueClass);
        this.ids = ids;
        this.properties = properties;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject value = context.getDataKeys().getValue(0);
        assert value.getType() instanceof FileClass;

        Object file = value.object;
        if (file instanceof byte[]) {
            try {
                if (value.getType() instanceof DynamicFormatFileClass) {
                    file = BaseUtils.getFile((byte[]) file);
                }
                ImportIterator iterator = getIterator((byte[]) file);

                List<String> row;
                int i = 0;
                while ((row = iterator.nextRow()) != null) {
                    DataObject rowKey = new DataObject(i, IntegerClass.instance);
                    LM.baseLM.imported.change(true, context, rowKey);
                    for (int j = 0; j < Math.min(properties.size(), row.size()); j++) {
                        LCP property = properties.get(j);
                        Type type = property.property.getType();
                        Object parsedObject = null;
                        try {
                            parsedObject = type.parseString(row.get(j));
                        } catch (lsfusion.server.data.type.ParseException ignored) {
                        }
                        property.change(parsedObject, context, rowKey);
                    }
                    i++;
                }
                
                iterator.release();
                
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
    }

    protected List<Integer> getSourceColumns(Map<String, Integer> mapping) {
        List<Integer> columns = new ArrayList<Integer>();
        int previousIndex = columnsNumberBase() - 1;
        for (String id : ids) {

            int currentIndex;
            if (id == null) {
                currentIndex = previousIndex + 1;
            } else {
                Integer desiredColumn = mapping.get(id);
                if (desiredColumn != null) {
                    currentIndex = desiredColumn;
                } else {
                    currentIndex = previousIndex + 1;
                }
            }
            columns.add(currentIndex);
            previousIndex = currentIndex;
        }

        return columns;
    }
    
    protected int columnsNumberBase() {
        return 0;
    }

    public abstract ImportIterator getIterator(byte[] file) throws IOException, ParseException, xBaseJException, JDOMException, ClassNotFoundException;
}
