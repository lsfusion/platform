package lsfusion.server.logics.form.open.stat;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.FormDataManager;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.stat.StaticFormDataManager;
import lsfusion.server.logics.form.stat.integration.FormIntegrationType;
import lsfusion.server.logics.form.stat.integration.exporting.StaticExportData;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;

public abstract class ExportActionProperty<O extends ObjectSelector> extends FormStaticActionProperty<O, FormIntegrationType> {
    protected final String charset;

    public ExportActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, String charset, ActionOrProperty... extraProps) {
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
