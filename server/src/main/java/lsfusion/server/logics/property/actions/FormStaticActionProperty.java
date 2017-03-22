package lsfusion.server.logics.property.actions;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.FormExportType;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.FormStaticType;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.SystemProperties;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.remote.FormReportManager;
import lsfusion.server.remote.InteractiveFormReportManager;
import lsfusion.server.remote.StaticFormReportManager;
import net.sf.jasperreports.engine.JRException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class FormStaticActionProperty<T extends FormStaticType> extends FormActionProperty {

    protected final T staticType;
    
    private final LCP formExportFile;
    
    public FormStaticActionProperty(LocalizedString caption,
                                    FormEntity form,
                                    List<ObjectEntity> objectsToSet,
                                    List<Boolean> nulls, 
                                    T staticType,
                                    LCP formExportFile, Property... extraProps) {
        super(caption, form, objectsToSet, nulls, false, extraProps);

        this.staticType = staticType;
        this.formExportFile = formExportFile;
    }
    
    protected abstract Map<String, byte[]> exportPlain(ReportGenerationData reportData) throws IOException; // multiple files
    protected abstract byte[] exportHierarchical(ReportGenerationData reportData) throws JRException, IOException, ClassNotFoundException; // single file    

    protected abstract void exportClient(ExecutionContext<ClassPropertyInterface> context, ReportGenerationData reportData, Map<String, String> reportPath) throws SQLException, SQLHandledException;


    @Override
    protected void executeCustom(ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        FormReportManager newFormManager;

        if(Settings.get().isUseInteractiveReportManagerInsteadOfStatic() && !SystemProperties.isDebug) {
            FormInstance newFormInstance = context.createFormInstance(form, mapObjectValues);
            
            if (!newFormInstance.areObjectsFound()) {
                context.requestUserInteraction(
                        new MessageClientAction(ThreadLocalContext.localize(LocalizedString.create("{form.navigator.form.do.not.fit.for.specified.parameters}")),
                                ThreadLocalContext.localize(form.caption)));
                return;
            }

            newFormManager = new InteractiveFormReportManager(newFormInstance);
        } else
            newFormManager = new StaticFormReportManager(form, BaseUtils.<ImMap<ObjectEntity, ObjectValue>>immutableCast(mapObjectValues), context);

        boolean isExcel = staticType instanceof FormPrintType && ((FormPrintType) staticType).isExcel();
        ReportGenerationData reportData = newFormManager.getReportData(isExcel, staticType instanceof FormExportType);

        if (formExportFile != null) {
            try {
                String extension = staticType.getExtension();
                if(staticType instanceof FormExportType && ((FormExportType) staticType).isPlain()) { // plain = multiple files
                    Map<String, byte[]> files = exportPlain(reportData);
                    for(Map.Entry<String, byte[]> entry : files.entrySet()) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(entry.getValue(), extension.getBytes()), context, new DataObject(entry.getKey()));
                    }
                } else { // hierarchical - single file
                    byte[] singleFile = exportHierarchical(reportData);
                    formExportFile.change(BaseUtils.mergeFileAndExtension(singleFile, extension.getBytes()), context);
                }
            } catch (JRException | IOException | ClassNotFoundException e) {
                ServerLoggers.systemLogger.error(e);
            }
        } else { // assert printType != null;
            Map<String, String> reportPath = SystemProperties.isDebug ? newFormManager.getReportPath(staticType instanceof FormPrintType && ((FormPrintType) staticType).isExcel(), null, null) : new HashMap<>();
            exportClient(context, reportData, reportPath);
        }
    }
}
