package lsfusion.server.logics.form.open.stat;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.print.FormStaticType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.open.FormAction;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.FormSelectTop;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public abstract class FormStaticAction<O extends ObjectSelector, T extends FormStaticType> extends FormAction<O> {

    protected final T staticType;

    protected final FormSelectTop<ClassPropertyInterface> selectTopInterfaces;

    public FormStaticAction(LocalizedString caption,
                            FormSelector<O> form,
                            ImList<O> objectsToSet,
                            ImList<Boolean> nulls,
                            ImOrderSet<PropertyInterface> orderContextInterfaces,
                            ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters,
                            T staticType,
                            FormSelectTop<ValueClass> selectTop,
                            ValueClass... extraValueClasses) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, null, selectTop, extraValueClasses);

        this.staticType = staticType;

        this.selectTopInterfaces = FormAction.getSelectTop(selectTop, getOrderInterfaces());
    }

    protected static void writeResult(LP<?> exportFile, FormStaticType staticType, ExecutionContext<ClassPropertyInterface> context, RawFileData singleFile, String charset) throws SQLException, SQLHandledException {
        exportFile.change(exportFile.property.getType().parseFile(singleFile, staticType.getExtension(), charset), context);
    }
}
