package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import jasperapi.ReportGenerator;
import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.form.PrintMessageData;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.SystemProperties;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.FormSelector;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.ObjectSelector;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.form.stat.StaticFormDataManager;
import lsfusion.server.form.stat.StaticFormReportManager;
import net.sf.jasperreports.engine.JRException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class PrintActionProperty<O extends ObjectSelector> extends FormStaticActionProperty<O, FormPrintType> {

    private CalcPropertyInterfaceImplement<ClassPropertyInterface> printerProperty;
    private CalcPropertyInterfaceImplement<ClassPropertyInterface> passwordProperty;

    private final LCP formPageCount;

    private final boolean syncType; // static interactive
    
    private final boolean removeNulls; // print message

    private final LCP sheetNameProperty;

    protected final LCP<?> exportFile; // nullable

    public PrintActionProperty(LocalizedString caption,
                               FormSelector<O> form,
                               final ImList<O> objectsToSet,
                               final ImList<Boolean> nulls,
                               FormPrintType staticType,
                               boolean syncType,
                               Integer top,
                               CalcProperty password,
                               LCP sheetNameProperty,
                               LCP exportFile,
                               CalcProperty printer,
                               LCP formPageCount, boolean removeNulls) {
        super(caption, form, objectsToSet, nulls, staticType, top, password, printer);

        this.formPageCount = formPageCount;

        this.syncType = syncType;
        
        this.removeNulls = removeNulls;
        
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
            PrintMessageData reportData = new StaticFormDataManager(form, mapObjectValues, context).getPrintMessageData(selectTop, removeNulls);

            // proceeding data
            LogMessageClientAction action = new LogMessageClientAction(reportData.message, reportData.titles, reportData.rows, !context.getSession().isNoCancelInTransaction());
            if(syncType)
                context.requestUserInteraction(action);
            else
                context.delayUserInteraction(action);
        } else {
            // getting data
            boolean isExcel = staticType.isExcel();
            StaticFormReportManager formReportManager = new StaticFormReportManager(form, mapObjectValues, context);
            ReportGenerationData reportData = formReportManager.getReportData(isExcel, selectTop);

            // proceeding data
            String password = passwordProperty == null ? null : (String) passwordProperty.read(context, context.getKeys());
            String sheetName = sheetNameProperty == null ? null : (String) sheetNameProperty.read(context);
            if (exportFile != null) {
                try {
                    writeResult(exportFile, staticType, context, IOUtils.getFileBytes(ReportGenerator.exportToFile(reportData, staticType, sheetName, password)));
                } catch (JRException | IOException | ClassNotFoundException e) {
                    throw Throwables.propagate(e);
                }
            } else {
                String pName = printerProperty == null ? null : (String) printerProperty.read(context, context.getKeys());
                List<ReportPath> customReportPathList = SystemProperties.inDevMode ? formReportManager.getCustomReportPathList(isExcel, null) : new ArrayList<ReportPath>();
                Integer pageCount = (Integer) context.requestUserInteraction(new ReportClientAction(customReportPathList, form.getSID(), syncType, reportData, staticType, pName, SystemProperties.inDevMode, password, sheetName));
                formPageCount.change(pageCount, context);
            }
        }
    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        if(exportFile != null)
            return getChangeProps(exportFile.property);
        return MapFact.EMPTY();
    }
}
