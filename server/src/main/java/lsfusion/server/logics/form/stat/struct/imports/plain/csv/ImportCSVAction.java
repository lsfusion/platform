package lsfusion.server.logics.form.stat.struct.imports.plain.csv;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportPlainAction;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportPlainIterator;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.IOException;

public class ImportCSVAction extends ImportPlainAction<ImportCSVIterator> {
    private boolean noHeader;
    private boolean noEscape;
    private String separator;
    
    public ImportCSVAction(int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity, String charset, boolean hasWhere, boolean noHeader, boolean noEscape, String separator) {
        super(paramsCount, groupFiles, formEntity, charset != null ? charset : ExternalUtils.defaultCSVCharset, hasWhere);
        this.noHeader = noHeader;
        this.noEscape = noEscape;
        this.separator = separator == null ? ExternalUtils.defaultCSVSeparator : separator;
    }

    @Override
    public ImportPlainIterator getIterator(RawFileData file, ImOrderMap<String, Type> fieldTypes, String wheres, ExecutionContext<PropertyInterface> context) throws IOException {
        return new ImportCSVIterator(fieldTypes, file, charset, wheres, noHeader, noEscape, separator);
    }

    protected boolean indexBased() {
        return noHeader;
    }
}