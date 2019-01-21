package lsfusion.gwt.server.convert;

import lsfusion.base.ProgressBar;
import lsfusion.client.logics.ClientFormChanges;
import lsfusion.client.logics.classes.ClientObjectClass;
import lsfusion.client.logics.classes.ClientTypeSerializer;
import lsfusion.gwt.shared.view.GProgressBar;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.provider.FormSessionObject;
import lsfusion.gwt.shared.view.actions.*;
import lsfusion.gwt.shared.view.changes.dto.GFormChangesDTO;
import lsfusion.gwt.shared.view.classes.GObjectClass;
import lsfusion.gwt.shared.view.classes.GType;
import lsfusion.gwt.shared.view.window.GModalityType;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.*;

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
    public GFormAction convertAction(FormClientAction action, FormSessionObject formSessionObject, LSFusionDispatchServlet servlet) throws IOException {
        GModalityType modalityType = convertOrCast(action.modalityType);
        return new GFormAction(modalityType, servlet.getFormProvider().createForm(action.canonicalName, action.formSID, action.remoteForm, action.immutableMethods, action.firstChanges, formSessionObject.sessionID),
                action.forbidDuplicate);
    }

    @Converter(from = ModalityType.class)
    public GModalityType convertModalityType(ModalityType modalityType) {
        switch (modalityType) {
            case DOCKED: return GModalityType.DOCKED;
            case MODAL: return GModalityType.MODAL;
            case DOCKED_MODAL: return GModalityType.DOCKED_MODAL;
            case DIALOG_MODAL: return GModalityType.DIALOG_MODAL;
        }
        return null;
    }

    @Converter(from = HideFormClientAction.class)
    public GHideFormAction convertAction(HideFormClientAction action) throws IOException {
        return new GHideFormAction();
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
        ClientFormChanges changes = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(action.formChanges)), form.clientForm);

        GFormChangesDTO changesDTO = valuesConverter.convertOrCast(changes, (int)action.requestIndex);

        return new GProcessFormChangesAction(changesDTO);
    }

    @Converter(from = ReportClientAction.class)
    public GReportAction convertAction(ReportClientAction action, FormSessionObject form) throws IOException {
        return new GReportAction(FileUtils.exportReport(action.printType, action.generationData));
    }

    @Converter(from = RequestUserInputClientAction.class)
    public GRequestUserInputAction convertAction(RequestUserInputClientAction action) throws IOException {
        GType type = typeConverter.convertOrCast(
                ClientTypeSerializer.deserializeClientType(action.readType)
        ) ;

        Object value = valuesConverter.convertOrCast(deserializeServerValue(action.oldValue));

        return new GRequestUserInputAction(type, value);
    }

    private Object deserializeServerValue(byte[] valueBytes) throws IOException {
        return valuesConverter.convertOrCast(deserializeObject(valueBytes));
    }

    @Converter(from = UpdateEditValueClientAction.class)
    public GUpdateEditValueAction convertAction(UpdateEditValueClientAction action) throws IOException {
        return new GUpdateEditValueAction(valuesConverter.convertOrCast(deserializeServerValue(action.value)));
    }

    @Converter(from = AsyncGetRemoteChangesClientAction.class)
    public GAsyncGetRemoteChangesAction convertAction(AsyncGetRemoteChangesClientAction action) throws IOException {
        return new GAsyncGetRemoteChangesAction();
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
        return new GOpenFileAction(FileUtils.saveFile(action.file, action.name, action.extension), action.name != null ? (action.name + (action.extension != null ? "." + action.extension : "")) : null);
    }

    @Converter(from = ExportFileClientAction.class)
    public GExportFileAction convertAction(ExportFileClientAction action) {
        ArrayList<String> filePaths = new ArrayList<>();
        for (String fileName : action.files.keySet()) {
            filePaths.add(FileUtils.saveFile(fileName, action.files.get(fileName)));
        }
        return new GExportFileAction(filePaths);
    }

    @Converter(from = ProgressBar.class)
    public GProgressBar convertProgressBar(ProgressBar progressBar) {
        return new GProgressBar(progressBar.message, progressBar.progress, progressBar.total, progressBar.params);
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
        return new GBeepAction(FileUtils.saveFile(action.file, "wav"));
    }

    @Converter(from = ActivateFormClientAction.class)
    public GActivateFormAction convertAction(ActivateFormClientAction action) {
        return new GActivateFormAction(action.formCanonicalName);
    }

    @Converter(from = MaximizeFormClientAction.class)
    public GMaximizeFormAction convertAction(MaximizeFormClientAction action) {
        return new GMaximizeFormAction();
    }
}
