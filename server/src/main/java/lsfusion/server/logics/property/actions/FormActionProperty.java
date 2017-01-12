package lsfusion.server.logics.property.actions;

import jasperapi.ReportGenerator;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.interop.FormExportType;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.ServerLoggers;
import lsfusion.server.SystemProperties;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.exporting.csv.CSVFormExporter;
import lsfusion.server.logics.property.actions.exporting.dbf.DBFFormExporter;
import lsfusion.server.logics.property.actions.exporting.json.JSONFormExporter;
import lsfusion.server.logics.property.actions.exporting.xml.XMLFormExporter;
import lsfusion.server.remote.FormReportManager;
import net.sf.jasperreports.engine.JRException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public class FormActionProperty extends SystemExplicitActionProperty {

    public final FormEntity<?> form;
    public final ImRevMap<ObjectEntity, ClassPropertyInterface> mapObjects;
    public final ObjectEntity input;

    private final boolean checkOnOk;
    private final Boolean manageSession;
    private final ModalityType modalityType;
    private final boolean showDrop;
    private final boolean isAdd;
    private final FormPrintType printType;
    private final FormExportType exportType;
    private final String charset;

    private final LCP formPageCount;
    private final LCP formExportFile;
    private final LCP formExportFiles;
    private final LCP ignorePrintType;

    private final ConcreteCustomClass formResultClass;
    private final LCP formResultProperty;
    private final AnyValuePropertyHolder chosenValueProperty;

    private final ObjectEntity contextObject;
    private final CalcPropertyMapImplement<PropertyInterface, ClassPropertyInterface> contextPropertyImplement;

    private final PropertyDrawEntity initFilterProperty;
    private final boolean readOnly;

    public FormPrintType getPrintType() {
        return printType;
    }

    private static ValueClass[] getValueClasses(ObjectEntity[] objects, CalcProperty contextProperty) {
        ValueClass[] valueClasses = new ValueClass[objects.length + (contextProperty == null ? 0 : contextProperty.interfaces.size())];
        for (int i = 0; i < objects.length; i++) {
            valueClasses[i] = objects[i].baseClass;
        }

        if (contextProperty != null) {
            ImMap<PropertyInterface, ValueClass> interfaceClasses = contextProperty.getInterfaceClasses(ClassType.formPolicy);
            ImOrderSet<PropertyInterface> propInterfaces = contextProperty.getFriendlyPropertyOrderInterfaces();
            for (int i = 0; i < propInterfaces.size(); ++i) {
                valueClasses[objects.length + i] = interfaceClasses.get(propInterfaces.get(i));
            }
        }

        return valueClasses;
    }

    @Override
    protected boolean allowNulls() { // temporary
        return allowNullValue;
    }

    //assert objects из form
    //assert getProperties одинаковой длины
    //getProperties привязаны к форме, содержащей свойство...
    public FormActionProperty(LocalizedString caption,
                              FormEntity form,
                              ObjectEntity input,
                              final ObjectEntity[] objectsToSet,
                              Boolean manageSession,
                              boolean isAdd,
                              ModalityType modalityType,
                              boolean checkOnOk,
                              boolean showDrop,
                              FormPrintType printType,
                              FormExportType exportType,
                              String charset,
                              ConcreteCustomClass formResultClass,
                              LCP formResultProperty,
                              LCP formPageCount,
                              LCP formExportFile,
                              LCP formExportFiles,
                              LCP ignorePrintType,
                              AnyValuePropertyHolder chosenValueProperty,
                              ObjectEntity contextObject,
                              CalcProperty contextProperty,
                              PropertyDrawEntity initFilterProperty, boolean readOnly, boolean allowNulls) {
        super(caption, getValueClasses(objectsToSet, contextProperty));
        
        this.allowNullValue = false; //allowNulls;
        
        this.input = input;
        
        this.formPageCount = formPageCount;
        this.formExportFile = formExportFile;
        this.formExportFiles = formExportFiles;
        this.ignorePrintType = ignorePrintType;

        this.formResultClass = formResultClass;
        this.formResultProperty = formResultProperty;
        this.chosenValueProperty = chosenValueProperty;

        this.modalityType = modalityType;
        this.checkOnOk = checkOnOk;
        this.showDrop = showDrop;
        this.printType = printType;
        this.exportType = exportType;
        this.charset = charset;
        this.manageSession = manageSession;
        this.isAdd = isAdd;

        this.contextObject = contextObject;
        this.initFilterProperty = initFilterProperty;
        this.readOnly = readOnly;

        this.contextPropertyImplement = contextProperty == null ? null : contextProperty.getImplement(
                getOrderInterfaces().subOrder(objectsToSet.length, interfaces.size())
        );

        mapObjects = getOrderInterfaces()
                .subOrder(0, objectsToSet.length)
                .mapOrderRevKeys(new GetIndex<ObjectEntity>() { // такой же дебилизм и в SessionDataProperty
                    public ObjectEntity getMapValue(int i) {
                        return objectsToSet[i];
                    }
                });
        this.form = form;
    }

    protected boolean isVolatile() {
        return true;
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        Result<ImSet<PullChangeProperty>> pullProps = new Result<>();
        ImSet<FilterEntity> contextFilters = null;
        if (contextPropertyImplement != null) {
            final CalcPropertyValueImplement<PropertyInterface> propertyValues = contextPropertyImplement.mapObjectValues(context.getKeys());
            if(propertyValues == null) { // вообще должно \ может проверяться на уровне allowNulls, но он целиком для всех параметров, поэтому пока так
                proceedNullException();
                return;
            }
            final FormInstance thisFormInstance = context.getFormInstance(false, true);
            contextFilters = thisFormInstance.getContextFilters(contextObject, propertyValues, context.getChangingPropertyToDraw(), pullProps);
        }

        final FormInstance newFormInstance = context.createFormInstance(form,
                                                                        mapObjects.join(context.getKeys()),
                                                                        context.getSession(),
                                                                        modalityType.isModal(),
                                                                        isAdd, manageSession,
                                                                        checkOnOk,
                                                                        showDrop,
                                                                        printType == null && exportType == null,
                                                                        contextFilters,
                                                                        initFilterProperty,
                                                                        pullProps.result,
                                                                        readOnly);

        if(exportType == null && printType == null) // inteaction
            context.requestFormUserInteraction(newFormInstance, modalityType, context.stack);
        else { // print / export
            if (!newFormInstance.areObjectsFound()) {
                context.requestUserInteraction(
                    new MessageClientAction(ThreadLocalContext.localize(LocalizedString.create("{form.navigator.form.do.not.fit.for.specified.parameters}")),
                            ThreadLocalContext.localize(form.caption)));
                return;
            }
            
            boolean toExcel = false;
            FormPrintType pType = printType;
            if(pType != null) {
                pType = ignorePrintType.read(context) != null ? FormPrintType.PRINT : printType;
                toExcel = pType == FormPrintType.XLS || pType == FormPrintType.XLSX;
            }
            
            FormReportManager newFormManager = new FormReportManager(newFormInstance);
            if (exportType != null) {
                try {
                    if (exportType == FormExportType.DOC) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(ReportGenerator.exportToDoc(newFormManager.getReportData(toExcel, false))), "doc".getBytes()), context);
                    } else if (exportType == FormExportType.DOCX) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(ReportGenerator.exportToDocx(newFormManager.getReportData(toExcel, false))), "docx".getBytes()), context);
                    } else if (exportType == FormExportType.PDF) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(ReportGenerator.exportToPdf(newFormManager.getReportData(toExcel, false))), "pdf".getBytes()), context);
                    } else if (exportType == FormExportType.XLS) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(ReportGenerator.exportToXls(newFormManager.getReportData(toExcel, false))), "xls".getBytes()), context);
                    } else if (exportType == FormExportType.XLSX) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(ReportGenerator.exportToXlsx(newFormManager.getReportData(toExcel, false))), "xlsx".getBytes()), context);
                    } else if (exportType == FormExportType.XML) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(new XMLFormExporter(newFormManager.getReportData(toExcel, true)).export(), "xml".getBytes()), context);
                    } else if (exportType == FormExportType.JSON) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(new JSONFormExporter(newFormManager.getReportData(toExcel, true)).export(), "json".getBytes()), context);
                    } else if (exportType == FormExportType.CSV) {
                        Map<String, byte[]> files = new CSVFormExporter(newFormManager.getReportData(toExcel, true)).export();
                        boolean first = true;
                        for(Map.Entry<String, byte[]> entry : files.entrySet()) {
                            byte[] fileBytes = BaseUtils.mergeFileAndExtension(entry.getValue(), "csv".getBytes());
                            if(first) {
                                formExportFile.change(fileBytes, context);
                                first = false;
                            }
                            formExportFiles.change(fileBytes, context, new DataObject(entry.getKey()));
                        }
                    } else if (exportType == FormExportType.DBF) {
                        Map<String, byte[]> files = new DBFFormExporter(newFormManager.getReportData(toExcel, true), charset).export();
                        boolean first = true;
                        for(Map.Entry<String, byte[]> entry : files.entrySet()) {
                            byte[] fileBytes = BaseUtils.mergeFileAndExtension(entry.getValue(), "dbf".getBytes());
                            if(first) {
                                formExportFile.change(fileBytes, context);
                                first = false;
                            }
                            formExportFiles.change(fileBytes, context, new DataObject(entry.getKey()));
                        }
                }
                } catch (JRException | IOException | ClassNotFoundException e) {
                    ServerLoggers.systemLogger.error(e);
                }
            } else { // assert printType != null;
                Map<String, String> reportPath = SystemProperties.isDebug ? newFormManager.getReportPath(toExcel, null, null) : new HashMap<>();
                Object pageCount = context.requestUserInteraction(new ReportClientAction(reportPath, modalityType.isModal(), newFormManager.getReportData(toExcel, false), pType, SystemProperties.isDebug));
                formPageCount.change(pageCount, context);
            }
        }

        if (modalityType.isModal()) {
            //для немодальных форм следующее бессмысленно, т.к. они остаются открытыми...

            FormCloseType formResult = newFormInstance.getFormResult();
            formResultProperty.change(formResultClass.getDataObject(formResult.asString()), context);

            for (GroupObjectEntity group : form.getGroupsIt()) {
                for (ObjectEntity object : group.getObjects()) {
                    chosenValueProperty.write(
                            object.baseClass.getType(), newFormInstance.instanceFactory.getInstance(object).getObjectValue(), context, new DataObject(object.getSID())
                    );
                }
            }
            
            if(input != null) {
                ObjectInstance object = newFormInstance.instanceFactory.getInstance(input);

                ObjectValue chosenValue = null;
                if(formResult != FormCloseType.CLOSE)
                    chosenValue = (formResult == FormCloseType.OK ? object.getObjectValue() : NullValue.instance);
                context.writeRequested(chosenValue, object.getType());
            }
        }
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return !isAdd;
    }
}
