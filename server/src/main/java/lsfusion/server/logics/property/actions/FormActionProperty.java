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
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.SystemProperties;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.remote.RemoteForm;
import net.sf.jasperreports.engine.JRException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public class FormActionProperty extends SystemExplicitActionProperty {

    public final FormEntity<?> form;
    public final ImRevMap<ObjectEntity, ClassPropertyInterface> mapObjects;

    private final boolean checkOnOk;
    private final boolean manageSession;
    private final ModalityType modalityType;
    private final boolean showDrop;
    private final boolean isAdd;
    private final FormPrintType printType;
    private final FormExportType exportType;

    private final ConcreteCustomClass formResultClass;
    private final LCP formResultProperty;
    private final LCP formPageCount;
    private final LCP formExportFile;
    private final LCP ignorePrintType;

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

    //assert objects из form
    //assert getProperties одинаковой длины
    //getProperties привязаны к форме, содержащей свойство...
    public FormActionProperty(LocalizedString caption,
                              FormEntity form,
                              final ObjectEntity[] objectsToSet,
                              boolean manageSession,
                              boolean isAdd,
                              ModalityType modalityType,
                              boolean checkOnOk,
                              boolean showDrop,
                              FormPrintType printType,
                              FormExportType exportType,
                              ConcreteCustomClass formResultClass,
                              LCP formResultProperty,
                              LCP formPageCount,
                              LCP formExportFile,
                              LCP ignorePrintType,
                              AnyValuePropertyHolder chosenValueProperty,
                              ObjectEntity contextObject,
                              CalcProperty contextProperty,
                              PropertyDrawEntity initFilterProperty, boolean readOnly) {
        super(caption, getValueClasses(objectsToSet, contextProperty));

        this.formResultClass = formResultClass;
        this.formResultProperty = formResultProperty;
        this.formPageCount = formPageCount;
        this.formExportFile = formExportFile;
        this.ignorePrintType = ignorePrintType;
        this.chosenValueProperty = chosenValueProperty;

        this.modalityType = modalityType;
        this.checkOnOk = checkOnOk;
        this.showDrop = showDrop;
        this.printType = printType;
        this.exportType = exportType;
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

    protected boolean allowNulls() {
        return false;
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        Result<ImSet<PullChangeProperty>> pullProps = new Result<>();
        ImSet<FilterEntity> contextFilters = null;
        ImMap<ClassPropertyInterface, DataObject> dataKeys = context.getDataKeys();
        if (contextPropertyImplement != null) {
            final CalcPropertyValueImplement<PropertyInterface> propertyValues = contextPropertyImplement.mapValues(dataKeys);
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
                                                                        printType == null,
                                                                        contextFilters,
                                                                        initFilterProperty,
                                                                        pullProps.result,
                                                                        readOnly);

        if (printType != null && !newFormInstance.areObjectsFound()) {
            context.requestUserInteraction(
                    new MessageClientAction(ThreadLocalContext.localize(LocalizedString.create("{form.navigator.form.do.not.fit.for.specified.parameters}")), 
                                            ThreadLocalContext.localize(form.caption)));
        } else {
            RemoteForm newRemoteForm = context.createRemoteForm(newFormInstance);
            if (exportType != null) {
                ReportGenerationData generationData = newRemoteForm.reportManager.getReportData();
                DataObject[] dataObjects = dataKeys.values().toArray(new DataObject[dataKeys.size()]);
                try {
                    if (exportType == FormExportType.DOC) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(ReportGenerator.exportToDoc(generationData)), "doc".getBytes()), context, dataObjects);
                    } else if (exportType == FormExportType.DOCX) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(ReportGenerator.exportToDocx(generationData)), "docx".getBytes()), context, dataObjects);
                    } else if (exportType == FormExportType.PDF) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(ReportGenerator.exportToPdf(generationData)), "pdf".getBytes()), context, dataObjects);
                    } else if (exportType == FormExportType.XLS) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(ReportGenerator.exportToXls(generationData)), "xls".getBytes()), context, dataObjects);
                    } else if (exportType == FormExportType.XLSX) {
                        formExportFile.change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(ReportGenerator.exportToXlsx(generationData)), "xlsx".getBytes()), context, dataObjects);
                    }
                } catch (JRException | IOException | ClassNotFoundException e) {
                    ServerLoggers.systemLogger.error(e);
                }
            }
            if (printType != null) {
                FormPrintType pType = ignorePrintType.read(context) != null ? FormPrintType.PRINT : printType;
                boolean toExcel = pType == FormPrintType.XLS || pType == FormPrintType.XLSX;
                Map<String, String> reportPath = SystemProperties.isDebug ? newRemoteForm.reportManager.getReportPath(toExcel, null, null) : new HashMap<>();
                Object pageCount = context.requestUserInteraction(new ReportClientAction(reportPath, modalityType.isModal(), newRemoteForm.reportManager.getReportData(toExcel), pType, SystemProperties.isDebug));
                formPageCount.change(pageCount, context);
            } else if (exportType == null) {
                context.requestUserInteraction(new FormClientAction(form.getCanonicalName(), form.getSID(), newRemoteForm, newRemoteForm.getImmutableMethods(), Settings.get().isDisableFirstChangesOptimization() ? null : newRemoteForm.getFormChangesByteArray(context.stack), modalityType));
            }

            if (modalityType.isModal()) {
                //для немодальных форм следующее бессмысленно, т.к. они остаются открытыми...

                FormCloseType formResult = newFormInstance.getFormResult();

                if (formResultProperty != null) {
                    formResultProperty.change(formResultClass.getDataObject(formResult.asString()), context);
                }

                if (chosenValueProperty != null) {
                    for (GroupObjectEntity group : form.getGroupsIt()) {
                        for (ObjectEntity object : group.getObjects()) {
                            chosenValueProperty.write(
                                    object.baseClass.getType(), newFormInstance.instanceFactory.getInstance(object).getObjectValue(), context, new DataObject(object.getSID())
                            );
                        }
                    }
                }

            }
        }
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return !isAdd;
    }
}
