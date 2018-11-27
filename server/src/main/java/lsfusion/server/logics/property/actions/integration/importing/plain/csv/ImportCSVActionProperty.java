package lsfusion.server.logics.property.actions.integration.importing.plain.csv;

import lsfusion.base.ExternalUtils;
import lsfusion.base.RawFileData;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportPlainActionProperty;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportPlainIterator;

import java.io.IOException;

public class ImportCSVActionProperty extends ImportPlainActionProperty<ImportCSVIterator> {
    private boolean noHeader;
    private boolean noEscape;
    private String separator;
    
    public ImportCSVActionProperty(int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity, String charset, boolean noHeader, boolean noEscape, String separator) {
        super(paramsCount, groupFiles, formEntity, charset != null ? charset : ExternalUtils.defaultCSVCharset);
        this.noHeader = noHeader;
        this.noEscape = noEscape;
        this.separator = separator == null ? ExternalUtils.defaultCSVSeparator : separator;
    }

    @Override
    public ImportPlainIterator getIterator(RawFileData file, ImOrderMap<String, Type> fieldTypes, ExecutionContext<PropertyInterface> context) throws IOException {
        return new ImportCSVIterator(fieldTypes, file, charset, noHeader, noEscape, separator);
    }

    protected boolean indexBased() {
        return noHeader;
    }
}