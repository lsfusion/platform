package lsfusion.server.logics.form.open.stat;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.FormDataManager;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.stat.StaticFormDataManager;
import lsfusion.server.logics.form.stat.integration.FormIntegrationType;
import lsfusion.server.logics.form.stat.integration.export.StaticExportData;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;

public abstract class ExportAction<O extends ObjectSelector> extends FormStaticAction<O, FormIntegrationType> {
    protected final String charset;

    public ExportAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, String charset, ActionOrProperty... extraProps) {
        super(caption, form, objectsToSet, nulls, staticType, null, extraProps);
        this.charset = charset;
    }
    
    protected abstract void export(ExecutionContext<ClassPropertyInterface> context, StaticExportData exportData, StaticDataGenerator.Hierarchy hierarchy) throws IOException, SQLException, SQLHandledException;

    @Override
    protected void executeInternal(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapResolvedObjects) throws SQLException, SQLHandledException {
        StaticFormDataManager formDataManager = new StaticFormDataManager(form, mapObjectValues, context);
        FormDataManager.ExportResult exportData = formDataManager.getExportData();
        try {
            export(context, new StaticExportData(exportData.keys, exportData.properties), exportData.hierarchy);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
