package lsfusion.server.logics.form.open.stat;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.interop.action.MessageClientType;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.interop.action.ServerPrintAction;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.SelectTop;
import lsfusion.server.logics.form.stat.StaticFormDataManager;
import lsfusion.server.logics.form.stat.print.PrintMessageData;
import lsfusion.server.logics.form.stat.print.StaticFormReportManager;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrintAction<O extends ObjectSelector> extends FormStaticAction<O, FormPrintType> {
    private final SelectTop<ClassPropertyInterface> selectTopInterfaces;

    private final ClassPropertyInterface printerInterface;
    private final ClassPropertyInterface sheetNameInterface;
    private final ClassPropertyInterface passwordInterface;

    private final LP formPageCount;

    private final boolean syncType; // static interactive
    private final MessageClientType messageType;
    
    private final boolean removeNullsAndDuplicates; // print message

    protected final LP<?> exportFile; // nullable
    private final boolean server;
    private final boolean autoPrint;

    public static ValueClass[] getExtraParams(SelectTop<ValueClass> selectTop, ValueClass printer, ValueClass sheetName, ValueClass password) {
        List<ValueClass> params = selectTop.getParams();
        if (printer != null)
            params.add(printer);
        if (sheetName != null)
            params.add(sheetName);
        if (password != null)
            params.add(password);
        return params.toArray(new ValueClass[0]);
    }
    public PrintAction(LocalizedString caption,
                       FormSelector<O> form,
                       final ImList<O> objectsToSet,
                       final ImList<Boolean> nulls,
                       ImOrderSet<PropertyInterface> orderContextInterfaces,
                       ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters,
                       FormPrintType staticType,
                       boolean server,
                       boolean syncType,
                       MessageClientType messageType,
                       boolean autoPrint,
                       LP exportFile,
                       LP formPageCount, boolean removeNullsAndDuplicates,
                       SelectTop<ValueClass> selectTop, ValueClass printer, ValueClass sheetName, ValueClass password,
                       ValueClass[] extraParams) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, selectTop, extraParams);

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();
        int shift = 0;
        this.passwordInterface = password != null ? orderInterfaces.get(orderInterfaces.size() - 1 - shift++) : null;
        this.sheetNameInterface = sheetName != null ? orderInterfaces.get(orderInterfaces.size() - 1 - shift++) : null;
        this.printerInterface = printer != null ? orderInterfaces.get(orderInterfaces.size() - 1 - shift++) : null;

        this.selectTopInterfaces = selectTop.mapValues(orderInterfaces, extraParams.length);

        this.formPageCount = formPageCount;

        this.syncType = syncType;
        this.messageType = messageType;
        
        this.removeNullsAndDuplicates = removeNullsAndDuplicates;
        
        this.exportFile = exportFile;
        this.server = server;
        this.autoPrint = autoPrint;
    }

    @Override
    protected void executeInternal(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapResolvedObjects, ImSet<ContextFilterInstance> contextFilters) throws SQLException, SQLHandledException {
        if (staticType == FormPrintType.MESSAGE) {
            // getting data
            PrintMessageData reportData = new StaticFormDataManager(form, mapObjectValues, context, contextFilters).getPrintMessageData(selectTopInterfaces.mapValues(context), removeNullsAndDuplicates);

            MessageClientType type = messageType;
            if(context.getSession().isNoCancelInTransaction())
                type = MessageClientType.INFO;

            // proceeding data
            context.message(context.getRemoteContext(), reportData.message, "lsFusion", reportData.rows, reportData.titles, type, !syncType);
        } else {
            // getting data
            StaticFormReportManager formReportManager = new StaticFormReportManager(form, mapObjectValues, context, contextFilters);
            ReportGenerationData reportData = formReportManager.getReportData(staticType, selectTop);

            String sheetName = sheetNameInterface != null ? (String) context.getKeyObject(sheetNameInterface) : null;
            String password = passwordInterface != null ? (String) context.getKeyObject(passwordInterface) : null;

            //printer and sheet/password options doesn't intersect
            String printer = printerInterface != null ? (String)context.getKeyObject(printerInterface) : null;
            if (server) {
                ServerPrintAction.autoPrintReport(reportData, printer, (e) -> ServerLoggers.printerLogger.error("ServerPrint error", e));
            } else if (exportFile != null)
                //on the app-server the jasper classes should already be loaded, so remoteLogic is null
                writeResult(exportFile, staticType, context, ReportGenerator.exportToFileByteArray(reportData, staticType, sheetName, password, null));
            else {
                String formCaption = staticType == FormPrintType.PRINT ? formReportManager.readFormCaption() : null;
                List<String> customReportPathList = SystemProperties.inDevMode && form.isNamed() && context.getBL().findForm(form.getCanonicalName()) != null ? formReportManager.getCustomReportPathList(staticType) : new ArrayList<>(); // checking that form is not in script, etc.
                Integer pageCount = (Integer) context.requestUserInteraction(new ReportClientAction(customReportPathList, formCaption, form.getSID(), autoPrint, syncType, reportData, staticType, printer, SystemProperties.inDevMode, password, sheetName));
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
