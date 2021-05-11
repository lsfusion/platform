package lsfusion.server.logics.form.stat.struct.export.plain.csv;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.stat.struct.export.StaticExportData;
import lsfusion.server.logics.form.stat.struct.export.plain.ExportPlainAction;
import lsfusion.server.logics.form.stat.struct.export.plain.ExportPlainWriter;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.base.BaseUtils.nvl;

public class ExportCSVAction<O extends ObjectSelector> extends ExportPlainAction<O> {
    
    // csv
    private final boolean noHeader;
    private final boolean noEscape;
    private final String separator;

    public ExportCSVAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                           ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<?, PropertyInterface, O>> contextFilters,
                           FormIntegrationType staticType, ImMap<GroupObjectEntity, LP> exportFiles, Integer selectTop, String charset, boolean noHeader,
                           String separator, boolean noEscape) {
        this(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFiles, selectTop, charset, noHeader, separator, noEscape, false);
    }

    public ExportCSVAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                           ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<?, PropertyInterface, O>> contextFilters,
                           FormIntegrationType staticType, ImMap<GroupObjectEntity, LP> exportFiles, Integer selectTop, String charset, boolean noHeader,
                           String separator, boolean noEscape, boolean useCaptionInsteadOfIntegrationSID) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFiles, selectTop,
                nvl(charset, ExternalUtils.defaultCSVCharset), useCaptionInsteadOfIntegrationSID);
        
        this.noHeader = noHeader;
        this.noEscape = noEscape;
        this.separator = separator != null ? separator : ExternalUtils.defaultCSVSeparator;
    }

    @Override
    protected ExportPlainWriter getWriter(ImOrderMap<String, Type> fieldTypes, boolean singleRow) throws IOException {
        return new ExportCSVWriter(fieldTypes, noHeader, noEscape, separator, charset);
    }

    public RawFileData exportReport(StaticExportData exportData, StaticDataGenerator.Hierarchy hierarchy) throws IOException, SQLException, SQLHandledException {
        Map<GroupObjectEntity, RawFileData> files = new HashMap<>();
        exportGroupData(hierarchy.getRoot(), SetFact.EMPTY(), hierarchy, files, exportData, null);
        return files.values().iterator().next();
    }
}
