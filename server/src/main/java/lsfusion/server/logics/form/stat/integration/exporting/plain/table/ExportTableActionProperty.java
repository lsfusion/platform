package lsfusion.server.logics.form.stat.integration.exporting.plain.table;

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

public class ExportTableActionProperty<O extends ObjectSelector> extends ExportPlainActionProperty<O> {

    public ExportTableActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, ImMap<GroupObjectEntity, LCP> exportFiles, String charset) {
        super(caption, form, objectsToSet, nulls, staticType, exportFiles, charset);
    }

    @Override
    protected ExportPlainWriter getWriter(ImOrderMap<String, Type> fieldTypes, boolean singleRow) throws IOException {
        return new ExportTableWriter(fieldTypes, singleRow);
    }
}
