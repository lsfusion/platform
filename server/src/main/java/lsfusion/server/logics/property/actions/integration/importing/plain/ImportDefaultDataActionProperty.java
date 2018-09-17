package lsfusion.server.logics.property.actions.integration.importing.plain;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.integration.importing.plain.csv.ImportCSVIterator;
import lsfusion.server.logics.property.actions.integration.importing.plain.dbf.ImportDBFIterator;
import lsfusion.server.logics.property.actions.integration.importing.plain.mdb.ImportMDBIterator;
import lsfusion.server.logics.property.actions.integration.importing.plain.table.ImportTableIterator;
import lsfusion.server.logics.property.actions.integration.importing.plain.xls.ImportXLSIterator;
import org.jdom.JDOMException;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class ImportDefaultDataActionProperty {

//    public ImportDefaultDataActionProperty(List<String> ids, ImList<LCP> properties, List<Boolean> nulls, BaseLogicsModule baseLM) {
//        super(1, ids, properties, nulls, baseLM);
//    }
//
//    @Override
//    public ImportPlainIterator getIterator(byte[] file, ImOrderMap<String, Type> fieldTypes, String extension, ExecutionContext<PropertyInterface> context) throws IOException, IncorrectFileException, ClassNotFoundException, JDOMException, JSONException {
//        switch (extension) {
//            case "xls":
//            case "xlsx":
//                return new ImportXLSIterator(fieldTypes, file, extension.equals("xlsx"), 0);
//            case "xml":
//                return new ImportXMLIterator(fieldTypes, file, null, false, false);
//            case "json":
//                return new ImportJSONIterator(fieldTypes, file, null, false);
//            case "csv":
//                return new ImportCSVIterator(fieldTypes, file, null, false, null);
//            case "dbf":
//                return new ImportDBFIterator(fieldTypes, file, null, null, null);
//            case "jdbc":
//                return new ImportTableIterator(fieldTypes, file);
//            case "mdb":
//                return new ImportMDBIterator(fieldTypes, file);
//            default:
//                throw new UnsupportedOperationException(String.format("Unsupported file format '%s'", extension));
//        }
//    }
}