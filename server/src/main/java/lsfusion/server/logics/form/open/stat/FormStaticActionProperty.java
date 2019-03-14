package lsfusion.server.logics.form.open.stat;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.form.stat.FormStaticType;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.classes.StaticFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.DataObject;
import lsfusion.server.logics.form.open.FormActionProperty;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyReaderEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

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
                                    ActionOrProperty... extraProps) {
        super(caption, form, objectsToSet, nulls, false, extraProps);

        this.staticType = staticType;
        this.selectTop = selectTop == null ? 0 : selectTop;
    }

    protected static void writeResult(LP<?> exportFile, FormStaticType staticType, ExecutionContext<ClassPropertyInterface> context, RawFileData singleFile, DataObject... params) throws SQLException, SQLHandledException {
        if (exportFile.property.getType() instanceof StaticFormatFileClass) {
            exportFile.change(singleFile, context, params);
        } else {
            exportFile.change(singleFile != null ? new FileData(singleFile, staticType.getExtension()) : null, context, params);
        }
    }

    @Override
    protected ImMap<Property, Boolean> aspectUsedExtProps() {
        FormEntity formEntity = form.getStaticForm();
        if(formEntity != null) {
            MSet<Property> mProps = SetFact.mSet();
            boolean isReport = this instanceof PrintActionProperty;
            for (PropertyDrawEntity<?> propertyDraw : formEntity.getStaticPropertyDrawsList()) {
                if (isReport) {
                    MExclSet<PropertyReaderEntity> mReaders = SetFact.mExclSet();
                    propertyDraw.fillQueryProps(mReaders);
                    for (PropertyReaderEntity reader : mReaders.immutable())
                        mProps.add((Property) reader.getPropertyObjectEntity().property);
                } else 
                    mProps.add(propertyDraw.getCalcValueProperty().property);
            }
            return mProps.immutable().toMap(false);
        }
        return super.aspectChangeExtProps();
    }

}
