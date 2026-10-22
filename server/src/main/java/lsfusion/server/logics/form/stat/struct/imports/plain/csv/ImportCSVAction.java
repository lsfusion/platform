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
    private final PropertyInterface separatorInterface;

    public ImportCSVAction(int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity, boolean hasWhere, boolean noHeader, boolean noEscape, boolean hasSeparator, boolean hasCharset) {
        super(paramsCount, groupFiles, formEntity, ExternalUtils.defaultCSVCharset, hasWhere, hasCharset);
        this.noHeader = noHeader;
        this.noEscape = noEscape;

        separatorInterface = hasSeparator ? getOrderInterfaces().get(groupFiles.size() + (hasWhere ? 1 : 0)) : null;
    }

    @Override
    public ImportPlainIterator getIterator(RawFileData file, ImOrderMap<String, Type> fieldTypes, String wheres, ExecutionContext<PropertyInterface> context) throws IOException {
        String separator = separatorInterface != null ? (String) context.getKeyObject(separatorInterface) : null;
        return new ImportCSVIterator(fieldTypes, file, getCharset(context), wheres, noHeader, noEscape, separator != null ? separator : ExternalUtils.defaultCSVSeparator);
    }

    protected boolean indexBased() {
        return noHeader;
    }
}