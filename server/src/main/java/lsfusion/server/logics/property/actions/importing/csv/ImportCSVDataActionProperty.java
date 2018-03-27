package lsfusion.server.logics.property.actions.importing.csv;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.util.List;

public class ImportCSVDataActionProperty extends ImportDataActionProperty {
    private String separator;
    private boolean noHeader;
    private String charset;

    public ImportCSVDataActionProperty(List<String> ids, ImOrderSet<LCP> properties,
                                       String separator, boolean noHeader, String charset, BaseLogicsModule baseLM) {
        super(1, ids, properties, baseLM);
        this.separator = separator;
        this.noHeader = noHeader;
        this.charset = charset;
    }

    @Override
    public ImportIterator getIterator(byte[] file, String extension) {
        return new ImportCSVIterator(file, getSourceColumns(XLSColumnsMapping), properties, charset, separator, noHeader);
    }
}
