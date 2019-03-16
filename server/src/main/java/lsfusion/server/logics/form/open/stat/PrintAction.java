package lsfusion.server.logics.form.open.stat;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.form.stat.report.FormPrintType;
import lsfusion.interop.form.stat.report.ReportGenerationData;
import lsfusion.interop.form.stat.report.ReportGenerator;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.StaticFormDataManager;
import lsfusion.server.logics.form.stat.report.PrintMessageData;
import lsfusion.server.logics.form.stat.report.StaticFormReportManager;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrintAction<O extends ObjectSelector> extends FormStaticAction<O, FormPrintType> {

    private PropertyInterfaceImplement<ClassPropertyInterface> printerProperty;
    private PropertyInterfaceImplement<ClassPropertyInterface> passwordProperty;

    private final LP formPageCount;

    private final boolean syncType; // static interactive
    
    private final boolean removeNullsAndDuplicates; // print message

    private final LP sheetNameProperty;

    protected final LP<?> exportFile; // nullable

    public PrintAction(LocalizedString caption,
                       FormSelector<O> form,
                       final ImList<O> objectsToSet,
                       final ImList<Boolean> nulls,
                       FormPrintType staticType,
                       boolean syncType,
                       Integer top,
                       Property password,
                       LP sheetNameProperty,
                       LP exportFile,
                       Property printer,
                       LP formPageCount, boolean removeNullsAndDuplicates) {
        super(caption, form, objectsToSet, nulls, staticType, top, password, printer);

        this.formPageCount = formPageCount;

        this.syncType = syncType;
        
        this.removeNullsAndDuplicates = removeNullsAndDuplicates;
        
        this.exportFile = exportFile;

        this.sheetNameProperty = sheetNameProperty;

        if (printer != null) {
            this.printerProperty = printer.getImplement(
                    getOrderInterfaces().subOrder(objectsToSet.size(), interfaces.size())
            );
        }
        if (password != null) {
            this.passwordProperty = password.getImplement(
                    getOrderInterfaces().subOrder(objectsToSet.size(), interfaces.size())
            );
        }
    }

    @Override
    protected void executeCustom(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapResolvedObjects) throws SQLException, SQLHandledException {
        if (staticType == FormPrintType.MESSAGE) {
            // getting data
            PrintMessageData reportData = new StaticFormDataManager(form, mapObjectValues, context).getPrintMessageData(selectTop, removeNullsAndDuplicates);

            // proceeding data
            LogMessageClientAction action = new LogMessageClientAction(reportData.message, reportData.titles, reportData.rows, !context.getSession().isNoCancelInTransaction());
            if(syncType)
                context.requestUserInteraction(action);
            else
                context.delayUserInteraction(action);
        } else {
            // getting data
            StaticFormReportManager formReportManager = new StaticFormReportManager(form, mapObjectValues, context);
            ReportGenerationData reportData = formReportManager.getReportData(staticType, selectTop);

            // proceeding data
            String password = passwordProperty == null ? null : (String) passwordProperty.read(context, context.getKeys());
            String sheetName = sheetNameProperty == null ? null : (String) sheetNameProperty.read(context);
            if (exportFile != null)
                writeResult(exportFile, staticType, context, ReportGenerator.exportToFileByteArray(reportData, staticType, sheetName, password));
            else {
                String pName = printerProperty == null ? null : (String) printerProperty.read(context, context.getKeys());
                List<ReportPath> customReportPathList = SystemProperties.inDevMode && form.isNamed() && context.getBL().findForm(form.getCanonicalName()) != null ? formReportManager.getCustomReportPathList(staticType) : new ArrayList<ReportPath>(); // checking that form is not in script, etc.
                Integer pageCount = (Integer) context.requestUserInteraction(new ReportClientAction(customReportPathList, form.getSID(), syncType, reportData, staticType, pName, SystemProperties.inDevMode, password, sheetName));
                formPageCount.change(pageCount, context);
            }
        }
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        if(exportFile != null)
            return getChangeProps(exportFile.property);
        return MapFact.EMPTY();
    }
}
