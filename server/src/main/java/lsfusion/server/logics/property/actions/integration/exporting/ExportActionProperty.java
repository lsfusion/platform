package lsfusion.server.logics.property.actions.integration.exporting;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.actions.integration.FormIntegrationType;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.stat.FormDataManager;
import lsfusion.server.form.stat.StaticDataGenerator;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.FormStaticActionProperty;
import lsfusion.server.form.stat.StaticFormDataManager;

import java.io.IOException;
import java.sql.SQLException;

public abstract class ExportActionProperty<O extends ObjectSelector> extends FormStaticActionProperty<O, FormIntegrationType> {
    protected final String charset;

    public ExportActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, String charset, Property... extraProps) {
        super(caption, form, objectsToSet, nulls, staticType, null, extraProps);
        this.charset = charset;
    }
    
    protected abstract void export(ExecutionContext<ClassPropertyInterface> context, StaticExportData exportData, StaticDataGenerator.Hierarchy hierarchy) throws IOException, SQLException, SQLHandledException;

    @Override
    protected void executeCustom(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapResolvedObjects) throws SQLException, SQLHandledException {
        StaticFormDataManager formDataManager = new StaticFormDataManager(form, mapObjectValues, context);
        FormDataManager.ExportResult exportData = formDataManager.getExportData();
        try {
            export(context, new StaticExportData(exportData.keys, exportData.properties), exportData.hierarchy);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
