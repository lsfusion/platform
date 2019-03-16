package lsfusion.server.logics.form.stat.integration.exporting.plain.csv;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.integration.FormIntegrationType;
import lsfusion.server.logics.form.stat.integration.exporting.plain.ExportPlainAction;
import lsfusion.server.logics.form.stat.integration.exporting.plain.ExportPlainWriter;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;

public class ExportCSVAction<O extends ObjectSelector> extends ExportPlainAction<O> {
    
    // csv
    private final boolean noHeader;
    private final boolean noEscape;
    private final String separator;

    public ExportCSVAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, ImMap<GroupObjectEntity, LP> exportFiles, String charset, boolean noHeader, String separator, boolean noEscape) {
        super(caption, form, objectsToSet, nulls, staticType, exportFiles, charset != null ? charset : ExternalUtils.defaultCSVCharset);
        
        this.noHeader = noHeader;
        this.noEscape = noEscape;
        this.separator = separator != null ? separator : ExternalUtils.defaultCSVSeparator;
    }

    @Override
    protected ExportPlainWriter getWriter(ImOrderMap<String, Type> fieldTypes, boolean singleRow) throws IOException {
        return new ExportCSVWriter(fieldTypes, noHeader, noEscape, separator, charset);
    }
}
