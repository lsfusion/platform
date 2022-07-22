package lsfusion.server.logics.form.stat.struct.export.plain.xls;

import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.stat.struct.export.plain.ExportPlainAction;
import lsfusion.server.logics.form.stat.struct.export.plain.ExportPlainWriter;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;

public class ExportXLSAction<O extends ObjectSelector> extends ExportPlainAction<O> {
    private boolean xlsx;
    private boolean noHeader;

    private ClassPropertyInterface sheetNameInterface;

    private static ValueClass[] getExtraParams(ValueClass sheetName) {
        return sheetName != null ? new ValueClass[] {sheetName} : new ValueClass[] {};
    }

    public ExportXLSAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                           ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters,
                           FormIntegrationType staticType, ImMap<GroupObjectEntity, LP> exportFiles, Integer selectTop, String charset, boolean xlsx,
                           boolean noHeader, ValueClass sheetName) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFiles, selectTop, charset, getExtraParams(sheetName));
        this.xlsx = xlsx;
        this.noHeader = noHeader;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();
        if (sheetName != null)
            this.sheetNameInterface = orderInterfaces.get(orderInterfaces.size() - 1);
    }

    @Override
    protected ExportPlainWriter getWriter(ExecutionContext<ClassPropertyInterface> context, ImOrderMap<String, Type> fieldTypes, boolean singleRow) throws IOException {
        String sheetName = sheetNameInterface == null ? null : (String) context.getKeyObject(sheetNameInterface);
        return new ExportXLSWriter(fieldTypes, xlsx, noHeader, sheetName);
    }
}