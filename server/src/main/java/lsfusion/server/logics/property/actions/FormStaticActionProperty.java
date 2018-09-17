package lsfusion.server.logics.property.actions;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.FormStaticType;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormSelector;
import lsfusion.server.form.entity.ObjectSelector;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.Property;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public abstract class FormStaticActionProperty<O extends ObjectSelector, T extends FormStaticType> extends FormActionProperty<O> {

    protected final T staticType;
    
    protected int selectTop;

    public FormStaticActionProperty(LocalizedString caption,
                                    FormSelector<O> form,
                                    ImList<O> objectsToSet,
                                    ImList<Boolean> nulls,
                                    T staticType,
                                    Integer selectTop,
                                    Property... extraProps) {
        super(caption, form, objectsToSet, nulls, false, extraProps);

        this.staticType = staticType;
        this.selectTop = selectTop == null ? 0 : selectTop;
    }

    protected static void writeResult(LCP<?> exportFile, FormStaticType staticType, ExecutionContext<ClassPropertyInterface> context, byte[] singleFile, DataObject... params) throws SQLException, SQLHandledException {
        if (exportFile.property.getType() instanceof StaticFormatFileClass) {
            exportFile.change(singleFile, context, params);
        } else {
            exportFile.change(BaseUtils.mergeFileAndExtension(singleFile, staticType.getExtension().getBytes()), context, params);
        }
    }
}
