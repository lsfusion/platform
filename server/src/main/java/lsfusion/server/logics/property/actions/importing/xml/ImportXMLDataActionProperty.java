package lsfusion.server.logics.property.actions.importing.xml;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import org.jdom.JDOMException;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public class ImportXMLDataActionProperty extends ImportDataActionProperty {
    String root;
    boolean attr;

    public ImportXMLDataActionProperty(int paramsCount, List<String> ids, List<LCP> properties, boolean attr, BaseLogicsModule baseLM) {
        super(paramsCount, ids, properties, baseLM);
        this.attr = attr;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        root = context.getKeys().size() > 1 ? (String) context.getKeys().getValue(1).getValue() : null;
        return super.aspectExecute(context);
    }

    @Override
    public ImportIterator getIterator(byte[] file) throws IOException, ParseException, JDOMException, ClassNotFoundException {
        return new ImportXMLIterator(file, properties, root, attr) {
            @Override
            public List<Integer> getColumns(Map<String, Integer> mapping) {
                return getSourceColumns(mapping);
            }
        };
    }

    @Override
    protected boolean ignoreIncorrectColumns() {
        return false;
    }
}
