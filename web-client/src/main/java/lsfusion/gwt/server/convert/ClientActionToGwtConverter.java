package lsfusion.gwt.server.convert;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.SystemUtils;
import lsfusion.base.file.*;
import lsfusion.client.classes.ClientObjectClass;
import lsfusion.client.classes.ClientTypeSerializer;
import lsfusion.client.form.ClientFormChanges;
import lsfusion.client.form.controller.remote.proxy.RemoteFormProxy;
import lsfusion.client.form.property.async.ClientAsyncSerializer;
import lsfusion.client.navigator.ClientNavigatorChanges;
import lsfusion.gwt.client.GFormChangesDTO;
import lsfusion.gwt.client.GNavigatorChangesDTO;
import lsfusion.gwt.client.action.*;
import lsfusion.gwt.client.classes.GObjectClass;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.navigator.window.GContainerShowFormType;
import lsfusion.gwt.client.navigator.window.GModalityShowFormType;
import lsfusion.gwt.client.navigator.window.GShowFormType;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.action.*;
import lsfusion.interop.form.ContainerShowFormType;
import lsfusion.interop.form.ModalityShowFormType;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.session.ExternalHttpMethod;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.interop.session.HttpClientAction;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.net.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.deserializeObject;

@SuppressWarnings("UnusedDeclaration")
public class ClientActionToGwtConverter extends ObjectConverter {
    private static final class InstanceHolder {
        private static final ClientActionToGwtConverter instance = new ClientActionToGwtConverter();
    }

    public static ClientActionToGwtConverter getInstance() {
        return InstanceHolder.instance;
    }

    private final ClientTypeToGwtConverter typeConverter = ClientTypeToGwtConverter.getInstance();
    private final ClientFormChangesToGwtConverter valuesConverter = ClientFormChangesToGwtConverter.getInstance();
    private final ClientNavigatorChangesToGwtConverter navigatorConverter = ClientNavigatorChangesToGwtConverter.getInstance();

    private ClientActionToGwtConverter() {
    }

    public GAction convertAction(ClientAction clientAction, Object... context) {
        return convertOrNull(clientAction, context);
    }

    @Converter(from = ChooseClassClientAction.class)
    public GChooseClassAction convertAction(ChooseClassClientAction action) throws IOException {
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(action.classes));

        GObjectClass baseClass = typeConverter.convertOrCast(
                (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inputStream)
        );

        GObjectClass defaultClass = typeConverter.convertOrCast(
                (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inputStream)
        );

        return new GChooseClassAction(action.concrete, baseClass, defaultClass);
    }

    @Converter(from = ConfirmClientAction.class)
    public GConfirmAction convertAction(ConfirmClientAction action) {
        return new GConfirmAction(action.message, action.caption, action.cancel, action.timeout, action.initialValue);
    }

    @Converter(from = FormClientAction.class)
    public GFormAction convertAction(FormClientAction action, FormSessionObject formSessionObject, String realHostName, MainDispatchServlet servlet) throws IOException {
        GShowFormType modalityType = convertOrCast(action.showFormType);
        RemoteFormInterface remoteForm = new RemoteFormProxy(action.remoteForm, realHostName);
        return new GFormAction(modalityType, servlet.getFormProvider().createForm(servlet, remoteForm, action.clientData, formSessionObject.navigatorID),
                action.forbidDuplicate, action.formId);
    }

    @Converter(from = ModalityShowFormType.class)
    public GModalityShowFormType convertModalityShowType(ModalityShowFormType modalityShowType) {
        GModalityShowFormType modalityType = null;
        switch (modalityShowType) {
            case DOCKED:
                modalityType = GModalityShowFormType.DOCKED;
                break;
            case MODAL:
                modalityType = GModalityShowFormType.MODAL;
                break;
            case DOCKED_MODAL:
                modalityType = GModalityShowFormType.DOCKED_MODAL;
                break;
            case DIALOG_MODAL:
                modalityType = GModalityShowFormType.DIALOG_MODAL;
                break;
            case EMBEDDED:
                modalityType = GModalityShowFormType.EMBEDDED;
                break;
            case POPUP:
                modalityType = GModalityShowFormType.POPUP;
                break;
        }
        return modalityType;
    }

    @Converter(from = ContainerShowFormType.class)
    public GContainerShowFormType convertModalityType(ContainerShowFormType containerShowTypeX) {
        return new GContainerShowFormType(containerShowTypeX.inContainerId);
    }

    @Converter(from = HideFormClientAction.class)
    public GHideFormAction convertAction(HideFormClientAction action, final String formID, final MainDispatchServlet servlet) {
        return new GHideFormAction();
    }

    @Converter(from = DestroyFormClientAction.class)
    public GDestroyFormAction convertAction(DestroyFormClientAction action, final String formID, final MainDispatchServlet servlet) {
        servlet.getFormProvider().scheduleRemoveFormSessionObject(formID, action.closeNotConfirmedDelay);
        return new GDestroyFormAction(action.closeConfirmedDelay);
    }

    @Converter(from = MessageClientAction.class)
    public GMessageAction convertAction(MessageClientAction action, FormSessionObject formSessionObject, MainDispatchServlet servlet) throws IOException {
        ArrayList<ArrayList<String>> arrayData = new ArrayList<>();
        for(List<String> row : action.data)
            arrayData.add(new ArrayList<>(row));
        return new GMessageAction(valuesConverter.convertFileValue(action.message, formSessionObject, servlet), action.textMessage, action.caption, arrayData, new ArrayList<>(action.titles), convertOrCast(action.type), action.syncType);
    }

    @Converter(from = MessageClientType.class)
    public GMessageType convertMessageClientType(MessageClientType messageClientType) {
        switch (messageClientType) {
            case LOG:
                return GMessageType.LOG;
            case INFO:
                return GMessageType.INFO;
            case SUCCESS:
                return GMessageType.SUCCESS;
            case WARN:
                return GMessageType.WARN;
            case ERROR:
                return GMessageType.ERROR;
            case DEFAULT:
                return GMessageType.DEFAULT;
        }
        throw new UnsupportedOperationException();
    }

    @Converter(from = ProcessFormChangesClientAction.class)
    public GProcessFormChangesAction convertAction(ProcessFormChangesClientAction action, FormSessionObject form, MainDispatchServlet servlet) throws IOException {
        ClientFormChanges changes = new ClientFormChanges(action.formChanges, form.clientForm);

        GFormChangesDTO changesDTO = valuesConverter.convertOrCast(changes, (int)action.requestIndex, form, servlet);

        return new GProcessFormChangesAction(changesDTO);
    }

    @Converter(from = ProcessNavigatorChangesClientAction.class)
    public GProcessNavigatorChangesAction convertAction(ProcessNavigatorChangesClientAction action, FormSessionObject form, MainDispatchServlet servlet) throws IOException {
        ClientNavigatorChanges changes = new ClientNavigatorChanges(action.navigatorChanges);

        GNavigatorChangesDTO navigatorChanges = navigatorConverter.convertOrCast(changes, servlet, form.navigatorID);

        return new GProcessNavigatorChangesAction(navigatorChanges);
    }

    public static FileData exportToFileByteArray(MainDispatchServlet servlet, ReportGenerationData generationData, FormPrintType printType, String sheetName, String password, boolean jasperReportsIgnorePageMargins) {
        return new FileData(ReportGenerator.exportToFileByteArray(generationData, printType, sheetName, password, jasperReportsIgnorePageMargins, servlet.getNavigatorProvider().getRemoteLogics()), printType.getExtension());
    }

    @Converter(from = ReportClientAction.class)
    public GReportAction convertAction(ReportClientAction action, final MainDispatchServlet servlet) throws IOException {
        FileData fileData = action.fileData;
        Integer autoPrintTimeout = action.autoPrintTimeout;
        if(fileData == null) {
            fileData = exportToFileByteArray(servlet, action.generationData, action.printType, action.sheetName, action.password, action.jasperReportsIgnorePageMargins);
            autoPrintTimeout = action.autoPrint && action.printType != FormPrintType.HTML ? fileData.getLength() / 15 : null;
        }

        return new GReportAction(FileUtils.exportFile(fileData), action.autoPrint, autoPrintTimeout);
    }

    @Converter(from = RequestUserInputClientAction.class)
    public GRequestUserInputAction convertAction(RequestUserInputClientAction action, FormSessionObject formSessionObject, MainDispatchServlet servlet) throws IOException {
        GType type = typeConverter.convertOrCast(
                ClientTypeSerializer.deserializeClientType(action.readType)
        ) ;

        Object value = deserializeServerValue(action.oldValue, formSessionObject, servlet);

        ClientAsyncToGwtConverter asyncConverter = new ClientAsyncToGwtConverter(servlet, formSessionObject);

        GInputList inputList = asyncConverter.convertOrCast(ClientAsyncSerializer.deserializeInputList(action.inputList));
        GInputListAction[] inputListActions = asyncConverter.convertOrCast(ClientAsyncSerializer.deserializeInputListActions(action.inputListActions));

        return new GRequestUserInputAction(type, value, action.hasOldValue, action.customChangeFunction, inputList, inputListActions);
    }

    private Object deserializeServerValue(byte[] valueBytes, FormSessionObject formSessionObject, MainDispatchServlet servlet) throws IOException {
        return valuesConverter.convertFileValue(deserializeObject(valueBytes), formSessionObject, servlet);
    }

    @Converter(from = UpdateEditValueClientAction.class)
    public GUpdateEditValueAction convertAction(UpdateEditValueClientAction action, FormSessionObject formSessionObject, MainDispatchServlet servlet) throws IOException {
        return new GUpdateEditValueAction(deserializeServerValue(action.value, formSessionObject, servlet));
    }

    @Converter(from = AsyncGetRemoteChangesClientAction.class)
    public GAsyncGetRemoteChangesAction convertAction(AsyncGetRemoteChangesClientAction action) throws IOException {
        return new GAsyncGetRemoteChangesAction(action.forceLocalEvents);
    }

    @Converter(from = LogOutClientAction.class)
    public GLogOutAction convertAction(LogOutClientAction action) {
        return new GLogOutAction();
    }

    @Converter(from = OpenUriClientAction.class)
    public GOpenUriAction convertAction(OpenUriClientAction action, FormSessionObject formSessionObject, MainDispatchServlet servlet) throws IOException {
        return new GOpenUriAction((Serializable) deserializeServerValue(action.uri, formSessionObject, servlet), action.noEncode);
    }

    @Converter(from = EditNotPerformedClientAction.class)
    public GEditNotPerformedAction convertAction(EditNotPerformedClientAction action) {
        return new GEditNotPerformedAction();
    }

    @Converter(from = OpenFileClientAction.class)
    public GOpenFileAction convertAction(OpenFileClientAction action) {
        return new GOpenFileAction(FileUtils.saveActionFile(action.file, action.extension, action.name));
    }

    //todo: isBlockingFileRead, isDialog
    @Converter(from = ReadClientAction.class)
    public GReadAction convertAction(ReadClientAction action) {
        return new GReadAction(action.sourcePath, action.isDynamicFormatFileClass);
    }

    @Converter(from = DeleteFileClientAction.class)
    public GDeleteFileAction convertAction(DeleteFileClientAction action) {
        return new GDeleteFileAction(action.source);
    }

    @Converter(from = FileExistsClientAction.class)
    public GFileExistsAction convertAction(FileExistsClientAction action) {
        return new GFileExistsAction(action.source);
    }

    @Converter(from = MkdirClientAction.class)
    public GMkDirAction convertAction(MkdirClientAction action) {
        return new GMkDirAction(action.source);
    }

    @Converter(from = MoveFileClientAction.class)
    public GMoveFileAction convertAction(MoveFileClientAction action) {
        return new GMoveFileAction(action.source, action.destination);
    }

    @Converter(from = ListFilesClientAction.class)
    public GListFilesAction convertAction(ListFilesClientAction action) {
        return new GListFilesAction(action.source, action.recursive);
    }

    //it is actually downloading the file, not opening it in the browser
    @Converter(from = WriteClientAction.class)
    public GWriteAction convertAction(WriteClientAction action) {
        return new GWriteAction(FileUtils.saveActionFile(action.file, action.extension, BaseUtils.getFileName(action.path)), BaseUtils.addExtension(action.path, action.extension), Base64.encodeBase64String(action.file.getBytes()));
    }

    //todo: directory, wait
    @Converter(from = RunCommandClientAction.class)
    public GRunCommandAction convertAction(RunCommandClientAction action) {
        return new GRunCommandAction(action.command);
    }

    @Converter(from = HttpClientAction.class)
    public GHttpClientAction convertAction(HttpClientAction action) {
        return new GHttpClientAction(convertMethod(action.method), action.connectionString, action.body, action.headers);
    }

    @Converter(from = ExternalHttpMethod.class)
    public GExternalHttpMethod convertMethod(ExternalHttpMethod method) {
        return GExternalHttpMethod.valueOf(method.name());
    }

    @Converter(from = LoadLinkClientAction.class)
    public GLoadLinkAction convertAction(LoadLinkClientAction action) throws IOException {
        return new GLoadLinkAction();
    }

    @Converter(from = CopyToClipboardClientAction.class)
    public GCopyToClipboardAction convertAction(CopyToClipboardClientAction action) {
        return new GCopyToClipboardAction(action.value);
    }

    @Converter(from = BeepClientAction.class)
    public GBeepAction convertAction(BeepClientAction action) {
        return new GBeepAction(FileUtils.saveActionFile(action.file, "wav", "beep"));
    }

    @Converter(from = ActivateFormClientAction.class)
    public GActivateFormAction convertAction(ActivateFormClientAction action) {
        return new GActivateFormAction(action.formCanonicalName);
    }

    @Converter(from = CloseFormClientAction.class)
    public GCloseFormAction convertAction(CloseFormClientAction action) {
        return new GCloseFormAction(action.formId);
    }

    @Converter(from = MaximizeFormClientAction.class)
    public GMaximizeFormAction convertAction(MaximizeFormClientAction action) {
        return new GMaximizeFormAction();
    }

    @Converter(from = ChangeColorThemeClientAction.class)
    public GChangeColorThemeAction convertAction(ChangeColorThemeClientAction action) {
        return new GChangeColorThemeAction(GColorTheme.valueOf(action.colorTheme.name()));
    }

    @Converter(from = ResetWindowsLayoutClientAction.class)
    public GResetWindowsLayoutAction convertAction(ResetWindowsLayoutClientAction action) {
        return new GResetWindowsLayoutAction();
    }

    private Map<String, String> fontFamilyMap = new HashMap<>();

    public static String convertUrl(String url, Result<String> rFileExtension) {
        int lastDot = url.lastIndexOf(";");
        if(lastDot >= 0) {
            String extension = url.substring(lastDot + 1);

            if(BaseUtils.isSimpleWord(extension)) {
                rFileExtension.set(extension);
                url = url.substring(0, lastDot);
            }
        }
        if(rFileExtension.result == null) {
            lastDot = url.lastIndexOf(".");
            if(lastDot >= 0) {
                String extension = url.substring(lastDot + 1);
                int query = extension.indexOf("?");
                if(query >= 0)
                    extension = extension.substring(0, query);

                if(BaseUtils.isSimpleWord(extension))
                    rFileExtension.set(extension);
            }
        }

        try {
            return URIUtil.encodeQuery(url, ExternalUtils.defaultUrlCharset.name());
        } catch (Throwable t) {
            throw Throwables.propagate(t);
        }
    }

    @Converter(from = ClientWebAction.class)
    public GClientWebAction convertAction(ClientWebAction action, FormSessionObject formSessionObject, MainDispatchServlet servlet) throws IOException {
        ArrayList<Serializable> values = new ArrayList<>();
        ArrayList<Object> types = new ArrayList<>();

        ArrayList<byte[]> actionTypes = action.types;
        ArrayList<byte[]> actionValues = action.values;

        for (int i = 0; i < actionTypes.size(); i++) {
            types.add(typeConverter.convertOrCast(ClientTypeSerializer.deserializeClientType(actionTypes.get(i))));
            values.add((Serializable) deserializeServerValue(actionValues.get(i), formSessionObject, servlet));
        }

        GType returnType = action.returnType != null ? typeConverter.convertOrCast(ClientTypeSerializer.deserializeClientType(action.returnType)) : null;

        Object resource = action.resource;
        String resourcePath;
        String originalResourceName = action.originalResourceName;
        String fileExtension = null;
        boolean isFileUrl = false;
        String resourceName = action.resourceName;
        if(action.isFile) {
            if(resource instanceof RawFileData) {
                resourcePath = FileUtils.saveWebFile(resourceName, (RawFileData) resource, servlet.getServerSettings(formSessionObject.navigatorID), false);
                fileExtension = BaseUtils.getFileExtension(resourceName);
            } else {
                Result<String> rFileExtension = new Result<>();
                // we do the server conversion (not sending GStringWithFiles to the client), to make it possible to encode the query and do some other manipulations
                // valuesConverter.convertFileValue(resource, formSessionObject, servlet)
                Object convertedValue = ClientFormChangesToGwtConverter.getConvertFileValue(formSessionObject, servlet).convertFileValue(resource);
                resourcePath = convertUrl((String) convertedValue, rFileExtension);
                fileExtension = rFileExtension.result;
                isFileUrl = true;
            }
        } else
            resourcePath = (String) resource;

        if(action.isFile && (BaseUtils.equalsIgnoreCase(fileExtension, "ttf") || BaseUtils.equalsIgnoreCase(fileExtension, "otf"))) {
            String fontFamily = fontFamilyMap.get(resourceName);
            if(fontFamily == null) {
                fontFamily = SystemUtils.registerFont(action);
                fontFamilyMap.put(resourceName, fontFamily);
            }
            originalResourceName = fontFamily;
        }

        return new GClientWebAction(resourcePath, resourceName, originalResourceName, action.isFile, fileExtension, isFileUrl, values, types, returnType,
                action.syncType, action.remove);
    }

    @Converter(from = ResetServerSettingsCacheClientAction.class)
    public void convertAction(ResetServerSettingsCacheClientAction action, MainDispatchServlet servlet) {
        servlet.getLogicsProvider().resetServerSettingsCache(servlet.getRequest());
    }

    @Converter(from = OrderClientAction.class)
    public GOrderAction convertAction(OrderClientAction action) {
        return new GOrderAction(action.goID, action.ordersMap);
    }

    @Converter(from = FilterClientAction.class)
    public GFilterAction convertAction(FilterClientAction action, FormSessionObject formSessionObject, MainDispatchServlet servlet) throws IOException {
        List<GFilterAction.FilterItem> filters = new ArrayList<>();
        for (FilterClientAction.FilterItem filter : action.filters) {
            filters.add(new GFilterAction.FilterItem(filter.propertyId, filter.compare, filter.negation, (Serializable) deserializeServerValue(filter.value, formSessionObject, servlet), filter.junction));
        }
        return new GFilterAction(action.goID, filters);
    }

    @Converter(from = FilterGroupClientAction.class)
    public GFilterGroupAction convertAction(FilterGroupClientAction action) {
        return new GFilterGroupAction(action.filterGroup, action.index);
    }
}
