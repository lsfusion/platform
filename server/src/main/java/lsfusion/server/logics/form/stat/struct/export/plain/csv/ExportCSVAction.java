package lsfusion.server.logics.form.stat.struct.export.plain.csv;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.stat.struct.export.plain.ExportPlainAction;
import lsfusion.server.logics.form.stat.struct.export.plain.ExportPlainWriter;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;

public class ExportCSVAction<O extends ObjectSelector> extends ExportPlainAction<O> {
    
    // csv
    private final boolean noHeader;
    private final boolean noEscape;
    private final String separator;

    public ExportCSVAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                           ImOrderSet<PropertyInterface> orderContextInterfaces, ImList<ContextFilterSelector<?, PropertyInterface, O>> contextFilters,
                           FormIntegrationType staticType, ImMap<GroupObjectEntity, LP> exportFiles, Integer selectTop, String charset, boolean noHeader, String separator, boolean noEscape) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFiles, selectTop, charset != null ? charset : ExternalUtils.defaultCSVCharset);
        
        this.noHeader = noHeader;
        this.noEscape = noEscape;
        this.separator = separator != null ? separator : ExternalUtils.defaultCSVSeparator;
    }

    @Override
    protected ExportPlainWriter getWriter(ImOrderMap<String, Type> fieldTypes, boolean singleRow) throws IOException {
        return new ExportCSVWriter(fieldTypes, noHeader, noEscape, separator, charset);
    }
}
