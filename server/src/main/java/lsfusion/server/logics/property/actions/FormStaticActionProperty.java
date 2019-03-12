package lsfusion.server.logics.property.actions;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import jasperapi.FormStaticType;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.Property;

import java.sql.SQLException;

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

    protected static void writeResult(LCP<?> exportFile, FormStaticType staticType, ExecutionContext<ClassPropertyInterface> context, RawFileData singleFile, DataObject... params) throws SQLException, SQLHandledException {
        if (exportFile.property.getType() instanceof StaticFormatFileClass) {
            exportFile.change(singleFile, context, params);
        } else {
            exportFile.change(singleFile != null ? new FileData(singleFile, staticType.getExtension()) : null, context, params);
        }
    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        FormEntity formEntity = form.getStaticForm();
        if(formEntity != null) {
            MSet<CalcProperty> mProps = SetFact.mSet();
            boolean isReport = this instanceof PrintActionProperty;
            for (PropertyDrawEntity<?> propertyDraw : formEntity.getStaticPropertyDrawsList()) {
                if (isReport) {
                    MExclSet<PropertyReaderEntity> mReaders = SetFact.mExclSet();
                    propertyDraw.fillQueryProps(mReaders);
                    for (PropertyReaderEntity reader : mReaders.immutable())
                        mProps.add((CalcProperty) reader.getPropertyObjectEntity().property);
                } else 
                    mProps.add(propertyDraw.getCalcValueProperty().property);
            }
            return mProps.immutable().toMap(false);
        }
        return super.aspectChangeExtProps();
    }

}
