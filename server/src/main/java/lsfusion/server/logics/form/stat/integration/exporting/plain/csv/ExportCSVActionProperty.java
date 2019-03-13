package lsfusion.server.logics.form.stat.integration.exporting.plain.csv;

import lsfusion.interop.session.ExternalUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.logics.form.stat.integration.FormIntegrationType;
import lsfusion.server.logics.form.stat.integration.exporting.plain.ExportPlainActionProperty;
import lsfusion.server.logics.form.stat.integration.exporting.plain.ExportPlainWriter;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LCP;

import java.io.IOException;

public class ExportCSVActionProperty<O extends ObjectSelector> extends ExportPlainActionProperty<O> {
    
    // csv
    private final boolean noHeader;
    private final boolean noEscape;
    private final String separator;

    public ExportCSVActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, ImMap<GroupObjectEntity, LCP> exportFiles, String charset, boolean noHeader, String separator, boolean noEscape) {
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
