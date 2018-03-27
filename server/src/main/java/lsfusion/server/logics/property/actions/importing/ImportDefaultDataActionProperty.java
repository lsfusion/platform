package lsfusion.server.logics.property.actions.importing;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.JDBCTable;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.csv.ImportCSVIterator;
import lsfusion.server.logics.property.actions.importing.dbf.CustomDbfReader;
import lsfusion.server.logics.property.actions.importing.dbf.ImportDBFDataActionProperty;
import lsfusion.server.logics.property.actions.importing.dbf.ImportDBFIterator;
import lsfusion.server.logics.property.actions.importing.table.ImportTableDataActionProperty;
import lsfusion.server.logics.property.actions.importing.table.ImportTableIterator;
import lsfusion.server.logics.property.actions.importing.json.ImportJSONIterator;
import lsfusion.server.logics.property.actions.importing.mdb.ImportMDBDataActionProperty;
import lsfusion.server.logics.property.actions.importing.mdb.ImportMDBIterator;
import lsfusion.server.logics.property.actions.importing.xls.ImportXLSIterator;
import lsfusion.server.logics.property.actions.importing.xls.ImportXLSXIterator;
import lsfusion.server.logics.property.actions.importing.xml.ImportXMLIterator;
import org.jdom.JDOMException;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImportDefaultDataActionProperty extends ImportDataActionProperty {

    public ImportDefaultDataActionProperty(List<String> ids, ImOrderSet<LCP> properties, BaseLogicsModule baseLM) {
        super(1, ids, properties, baseLM);
    }

    @Override
    public ImportIterator getIterator(byte[] file, String extension) throws IOException, IncorrectFileException, ClassNotFoundException, JDOMException, JSONException {
        switch (extension) {

            case "xls":
                return new ImportXLSIterator(file, getSourceColumns(XLSColumnsMapping), properties, null);
            case "xlsx":
                return new ImportXLSXIterator(file, getSourceColumns(XLSColumnsMapping), properties, null);
            case "xml":
                return new ImportXMLIterator(file, properties, ids, null, hasListOption, false) {
                    @Override
                    public List<Integer> getColumns(Map<String, Integer> mapping) {
                        return getSourceColumns(mapping);
                    }
                };
            case "json":
                return new ImportJSONIterator(file, properties, null, hasListOption) {
                    @Override
                    public List<Integer> getColumns(Map<String, Integer> mapping) {
                        return getSourceColumns(mapping);
                    }
                };
            case "csv":
                return new ImportCSVIterator(file, getSourceColumns(XLSColumnsMapping), properties, null, null, false);
            case "dbf":
                CustomDbfReader reader = new CustomDbfReader(new ByteArrayInputStream(file), null);
                return new ImportDBFIterator(reader, getSourceColumns(ImportDBFDataActionProperty.getFieldMapping(reader)), new ArrayList<List<String>>(), properties, null, null);
            case "jdbc":
                JDBCTable rs = JDBCTable.deserializeJDBC(file);
                return new ImportTableIterator(rs, getSourceColumns(ImportTableDataActionProperty.getFieldMapping(rs)), properties);
            case "mdb":
                List<Map<String, Object>> rows = (List<Map<String, Object>>) BaseUtils.deserializeCustomObject(file);
                return new ImportMDBIterator(ImportMDBDataActionProperty.getRowsList(rows), getSourceColumns(ImportMDBDataActionProperty.getFieldMapping(rows)));
            default:
                throw new UnsupportedOperationException(String.format("Unsupported file format '%s'", extension));
        }
    }
}