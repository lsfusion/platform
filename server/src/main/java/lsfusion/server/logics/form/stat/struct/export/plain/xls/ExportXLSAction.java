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
import java.util.ArrayList;
import java.util.List;

public class ExportXLSAction<O extends ObjectSelector> extends ExportPlainAction<O> {
    private boolean xlsx;
    private boolean noHeader;

    private ClassPropertyInterface sheetNameInterface;

    private static ValueClass[] getExtraParams(ValueClass selectTop, ImOrderMap<GroupObjectEntity, ValueClass> selectTops, ValueClass sheetName) {
        List<ValueClass> params = new ArrayList<>();
        if(selectTop != null)
            params.add(selectTop);
        if(selectTops != null)
            params.addAll(selectTops.values().toJavaCol());
        if(sheetName != null)
            params.add(sheetName);
        return params.toArray(new ValueClass[0]);
    }

    public ExportXLSAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                           ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters,
                           FormIntegrationType staticType, ImMap<GroupObjectEntity, LP> exportFiles, ValueClass selectTop, ImOrderMap<GroupObjectEntity, ValueClass> selectTops, String charset, boolean xlsx,
                           boolean noHeader, ValueClass sheetName) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFiles, selectTop, selectTops, charset, getExtraParams(selectTop, selectTops, sheetName));
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