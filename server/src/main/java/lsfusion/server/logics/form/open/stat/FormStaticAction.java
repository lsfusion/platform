package lsfusion.server.logics.form.open.stat;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.print.FormStaticType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.file.StaticFormatFileClass;
import lsfusion.server.logics.form.open.FormAction;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public abstract class FormStaticAction<O extends ObjectSelector, T extends FormStaticType> extends FormAction<O> {

    protected final T staticType;
    
    protected int selectTop;

    public FormStaticAction(LocalizedString caption,
                            FormSelector<O> form,
                            ImList<O> objectsToSet,
                            ImList<Boolean> nulls,
                            ImOrderSet<PropertyInterface> orderContextInterfaces,
                            ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters,
                            T staticType,
                            Integer selectTop,
                            ValueClass... extraValueClasses) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, null, extraValueClasses);

        this.staticType = staticType;
        this.selectTop = selectTop == null ? 0 : selectTop;
    }

    protected static void writeResult(LP<?> exportFile, FormStaticType staticType, ExecutionContext<ClassPropertyInterface> context, RawFileData singleFile) throws SQLException, SQLHandledException {
        exportFile.change(exportFile.property.getType().writeProp(singleFile, staticType.getExtension()), context);
    }
}
