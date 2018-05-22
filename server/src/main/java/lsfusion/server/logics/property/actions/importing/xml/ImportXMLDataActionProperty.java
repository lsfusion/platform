package lsfusion.server.logics.property.actions.importing.xml;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
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
import java.util.List;
import java.util.Map;

public class ImportXMLDataActionProperty extends ImportDataActionProperty {
    String root;
    boolean attr;

    public ImportXMLDataActionProperty(int paramsCount, List<String> ids, ImOrderSet<LCP> properties, List<Boolean> nulls, boolean hasListOption, boolean attr, BaseLogicsModule baseLM) {
        super(paramsCount, ids, properties, nulls, hasListOption, baseLM);
        this.attr = attr;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        root = context.getKeys().size() > 1 ? (String) context.getKeys().getValue(1).getValue() : null;
        return super.aspectExecute(context);
    }

    @Override
    public ImportIterator getIterator(byte[] file, String extension) throws IOException, JDOMException {
        return new ImportXMLIterator(file, properties, ids, root, hasListOption, attr) {
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
