package lsfusion.server.logics.property.actions.integration.exporting.plain.xls;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.FormSelector;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectSelector;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.integration.FormIntegrationType;
import lsfusion.server.logics.property.actions.integration.exporting.plain.ExportPlainActionProperty;
import lsfusion.server.logics.property.actions.integration.exporting.plain.ExportPlainWriter;

import java.io.IOException;

public class ExportXLSActionProperty<O extends ObjectSelector> extends ExportPlainActionProperty<O> {
    private boolean xlsx;
    private boolean noHeader;

    public ExportXLSActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, ImMap<GroupObjectEntity, LCP> exportFiles, String charset, boolean xlsx, boolean noHeader) {
        super(caption, form, objectsToSet, nulls, staticType, exportFiles, charset);
        this.xlsx = xlsx;
        this.noHeader = noHeader;
    }

    @Override
    protected ExportPlainWriter getWriter(ImOrderMap<String, Type> fieldTypes, boolean singleRow) throws IOException {
        return new ExportXLSWriter(fieldTypes, xlsx, noHeader);
    }
}