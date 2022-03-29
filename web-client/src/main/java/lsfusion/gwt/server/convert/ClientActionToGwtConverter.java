package lsfusion.gwt.server.convert;

import lsfusion.base.Pair;
import lsfusion.base.file.WriteClientAction;
import lsfusion.client.classes.ClientObjectClass;
import lsfusion.client.classes.ClientTypeSerializer;
import lsfusion.client.form.ClientFormChanges;
import lsfusion.client.form.controller.remote.proxy.RemoteFormProxy;
import lsfusion.client.form.property.async.ClientAsyncSerializer;
import lsfusion.gwt.client.GFormChangesDTO;
import lsfusion.gwt.client.action.*;
import lsfusion.gwt.client.base.GAsync;
import lsfusion.gwt.client.base.GProgressBar;
import lsfusion.gwt.client.classes.GObjectClass;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.navigator.window.GModalityType;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.ProgressBar;
import lsfusion.interop.action.*;
import lsfusion.interop.form.ModalityType;
import lsfusion.client.form.property.cell.ClientAsync;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.session.ExternalHttpMethod;
import lsfusion.interop.session.HttpClientAction;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private final ClientAsyncToGwtConverter asyncConverter = ClientAsyncToGwtConverter.getInstance();

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
        GModalityType modalityType = convertOrCast(action.modalityType);
        RemoteFormInterface remoteForm = new RemoteFormProxy(action.remoteForm, realHostName);
        return new GFormAction(modalityType, servlet.getFormProvider().createForm(action.canonicalName, action.formSID, remoteForm, action.immutableMethods, action.firstChanges, formSessionObject.navigatorID),
                action.forbidDuplicate);
    }

    @Converter(from = ModalityType.class)
    public GModalityType convertModalityType(ModalityType modalityType) {
        switch (modalityType) {
            case DOCKED: return GModalityType.DOCKED;
            case MODAL: return GModalityType.MODAL;
            case DOCKED_MODAL: return GModalityType.DOCKED_MODAL;
            case DIALOG_MODAL: return GModalityType.DIALOG_MODAL;
            case EMBEDDED: return GModalityType.EMBEDDED;
            case POPUP: return GModalityType.POPUP;
        }
        return null;
    }

    @Converter(from = HideFormClientAction.class)
    public GHideFormAction convertAction(HideFormClientAction action, final String formID, final MainDispatchServlet servlet) {
        servlet.getFormProvider().scheduleRemoveFormSessionObject(formID, action.closeNotConfirmedDelay);
        return new GHideFormAction(action.closeConfirmedDelay);
    }

    @Converter(from = LogMessageClientAction.class)
    public GLogMessageAction convertAction(LogMessageClientAction action) throws IOException {
        ArrayList<ArrayList<String>> arrayData = new ArrayList<>();
        for(List<String> row : action.data)
            arrayData.add(new ArrayList<>(row));
        return new GLogMessageAction(action.failed, action.message, arrayData, new ArrayList<>(action.titles));
    }

    @Converter(from = MessageClientAction.class)
    public GMessageAction convertAction(MessageClientAction action) {
        return new GMessageAction(action.message, action.caption);
    }

    @Converter(from = ProcessFormChangesClientAction.class)
    public GProcessFormChangesAction convertAction(ProcessFormChangesClientAction action, FormSessionObject form) throws IOException {
        ClientFormChanges changes = new ClientFormChanges(action.formChanges, form.clientForm);

        GFormChangesDTO changesDTO = valuesConverter.convertOrCast(changes, (int)action.requestIndex, form);

        return new GProcessFormChangesAction(changesDTO);
    }

    @Converter(from = ReportClientAction.class)
    public GReportAction convertAction(ReportClientAction action, final MainDispatchServlet servlet) throws IOException {
        Pair<String, String> report = FileUtils.exportReport(action.printType, action.generationData, servlet.getNavigatorProvider().getRemoteLogics());
        return new GReportAction(report.first, report.second);
    }

    @Converter(from = RequestUserInputClientAction.class)
    public GRequestUserInputAction convertAction(RequestUserInputClientAction action) throws IOException {
        GType type = typeConverter.convertOrCast(
                ClientTypeSerializer.deserializeClientType(action.readType)
        ) ;

        Object value = deserializeServerValue(action.oldValue);

        GInputList inputList = asyncConverter.convertOrCast(ClientAsyncSerializer.deserializeInputList(action.inputList));

        return new GRequestUserInputAction(type, value, action.hasOldValue, action.customChangeFunction, inputList);
    }

    private Object deserializeServerValue(byte[] valueBytes) throws IOException {
        return valuesConverter.convertOrCast(deserializeObject(valueBytes));
    }

    @Converter(from = UpdateEditValueClientAction.class)
    public GUpdateEditValueAction convertAction(UpdateEditValueClientAction action) throws IOException {
        return new GUpdateEditValueAction(deserializeServerValue(action.value));
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
    public GOpenUriAction convertAction(OpenUriClientAction action) {
        return new GOpenUriAction(action.uri.toString());
    }

    @Converter(from = EditNotPerformedClientAction.class)
    public GEditNotPerformedAction convertAction(EditNotPerformedClientAction action) {
        return new GEditNotPerformedAction();
    }

    @Converter(from = OpenFileClientAction.class)
    public GOpenFileAction convertAction(OpenFileClientAction action) {
        return new GOpenFileAction(FileUtils.saveActionFile(action.file), action.name, action.extension);
    }

    @Converter(from = WriteClientAction.class)
    public GOpenFileAction convertAction(WriteClientAction action) {
        return new GOpenFileAction(FileUtils.saveActionFile(action.file), action.path, action.extension);
    }

    @Converter(from = HttpClientAction.class)
    public GHttpClientAction convertAction(HttpClientAction action) {
        return new GHttpClientAction(convertMethod(action.method), action.connectionString, action.body, action.headers);
    }

    @Converter(from = ExternalHttpMethod.class)
    public GExternalHttpMethod convertMethod(ExternalHttpMethod method) {
        return GExternalHttpMethod.valueOf(method.name());
    }

    @Converter(from = ProgressBar.class)
    public GProgressBar convertProgressBar(ProgressBar progressBar) {
        return new GProgressBar(progressBar.message, progressBar.progress, progressBar.total, progressBar.getParams());
    }

    @Converter(from = ClientAsync.class)
    public GAsync convertAsync(ClientAsync async) {
        if(async.equals(ClientAsync.CANCELED))
            return GAsync.CANCELED;
        if(async.equals(ClientAsync.RECHECK))
            return GAsync.RECHECK;
        return new GAsync(async.displayString, async.rawString, valuesConverter.convertOrCast(async.key));
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
        return new GBeepAction(FileUtils.saveActionFile(action.file));
    }

    @Converter(from = ActivateFormClientAction.class)
    public GActivateFormAction convertAction(ActivateFormClientAction action) {
        return new GActivateFormAction(action.formCanonicalName);
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

    @Converter(from = ClientJSAction.class)
    public GClientJSAction convertAction(ClientJSAction action) throws IOException {
        ArrayList<Object> values = new ArrayList<>();
        ArrayList<Object> types = new ArrayList<>();

        ArrayList<byte[]> actionTypes = action.types;
        ArrayList<byte[]> actionValues = action.values;

        for (int i = 0; i < actionTypes.size(); i++) {
            types.add(typeConverter.convertOrCast(ClientTypeSerializer.deserializeClientType(actionTypes.get(i))));
            values.add(deserializeServerValue(actionValues.get(i)));
        }

        return new GClientJSAction(action.resource, action.resourceName,  values, types, action.isFile, action.syncType);
    }
}
