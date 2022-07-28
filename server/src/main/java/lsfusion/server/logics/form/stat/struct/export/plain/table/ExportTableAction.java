package lsfusion.server.logics.form.stat.struct.export.plain.table;

import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
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

public class ExportTableAction<O extends ObjectSelector> extends ExportPlainAction<O> {

    public ExportTableAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                             ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters,
                             FormIntegrationType staticType, ImMap<GroupObjectEntity, LP> exportFiles, Integer selectTop, String charset) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFiles, selectTop, charset);
    }

    @Override
    protected ExportPlainWriter getWriter(ExecutionContext<ClassPropertyInterface> context, ImOrderMap<String, Type> fieldTypes, boolean singleRow) throws IOException {
        return new ExportTableWriter(fieldTypes, singleRow);
    }
}
