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

public class ImportCSVActionProperty extends ImportPlainActionProperty<ImportCSVIterator> {
    private boolean noHeader;
    private String charset;
    private String separator;
    
    public ImportCSVActionProperty(int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity, boolean noHeader, String charset, String separator) {
        super(paramsCount, groupFiles, formEntity);
        this.noHeader = noHeader;
        this.charset = charset == null ? ExternalUtils.defaultCSVCharset : charset;
        this.separator = separator == null ? ExternalUtils.defaultCSVSeparator : separator;
    }

    @Override
    public ImportPlainIterator getIterator(RawFileData file, ImOrderMap<String, Type> fieldTypes, ExecutionContext<PropertyInterface> context) {
        return new ImportCSVIterator(fieldTypes, file, charset, noHeader, separator);
    }

    protected boolean indexBased() {
        return noHeader;
    }
}