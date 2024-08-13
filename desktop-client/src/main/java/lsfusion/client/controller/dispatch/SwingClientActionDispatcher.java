package lsfusion.client.controller.dispatch;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.base.file.FileDialogUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.exception.ClientExceptionManager;
import lsfusion.client.base.log.Log;
import lsfusion.client.classes.ClientObjectClass;
import lsfusion.client.classes.ClientTypeSerializer;
import lsfusion.client.controller.MainController;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.controller.remote.RmiRequest;
import lsfusion.client.controller.remote.proxy.RemoteObjectProxy;
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.classes.view.ClassDialog;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.remote.proxy.RemoteFormProxy;
import lsfusion.client.form.view.ClientFormDockable;
import lsfusion.client.form.view.ClientModalForm;
import lsfusion.client.navigator.controller.AsyncFormController;
import lsfusion.client.view.DockableMainFrame;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.action.*;
import lsfusion.interop.base.remote.PendingRemoteInterface;
import lsfusion.interop.base.remote.RemoteRequestInterface;
import lsfusion.interop.form.ModalityShowFormType;
import lsfusion.interop.form.ShowFormType;
import lsfusion.interop.form.event.EventBus;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerator;
import net.sf.jasperreports.engine.JRException;
import org.apache.commons.io.FileUtils;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterAbortException;
import java.io.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.swing.BorderFactory.createEmptyBorder;
import static lsfusion.client.ClientResourceBundle.getString;

public abstract class SwingClientActionDispatcher implements ClientActionDispatcher, DispatcherInterface {
    private EventObject editEvent;

    private boolean dispatchingPaused;

    private ServerResponse currentServerResponse = null;
    Object[] currentActionResults = null;
    private int currentActionIndex = -1;
    private int currentContinueIndex = -1;

    protected DispatcherListener dispatcherListener;

    public SwingClientActionDispatcher(DispatcherListener dispatcherListener) {
        this.dispatcherListener = dispatcherListener;
    }

    protected void onServerResponse(ServerResponse serverResponse) {
        //FormClientAction closes asyncForm, if there is no GFormAction in response,
        //we should close this erroneous asyncForm
        long requestIndex = serverResponse.requestIndex;
        AsyncFormController asyncFormController = getAsyncFormController(requestIndex);
        if (asyncFormController.onServerInvocationResponse()) { // if there are no docked form openings (which will eventually remove asyncForm), removing async forms explicitly
            if (Arrays.stream(serverResponse.actions).noneMatch(a -> (a instanceof FormClientAction && !getShowFormType((FormClientAction) a).isWindow()))) {
                ClientFormDockable formContainer = asyncFormController.removeAsyncForm();
                formContainer.onClosing();
            }
        }
    }

    public void dispatchServerResponse(ServerResponse serverResponse) throws IOException {
        onServerResponse(serverResponse);

        dispatchResponse(serverResponse);
    }
    public void dispatchResponse(ServerResponse serverResponse) throws IOException {
        assert serverResponse != null;

        int continueIndex = -1;
        try {
            do {
                Object[] actionResults = null;
                Throwable actionThrowable = null;
                ClientAction[] actions = serverResponse.actions;
                if (actions != null) {
                    int beginIndex;
                    if (dispatchingPaused) {
                        beginIndex = currentActionIndex + 1;
                        actionResults = currentActionResults;
                        continueIndex = currentContinueIndex;

                        currentActionIndex = -1;
                        currentActionResults = null;
                        currentServerResponse = null;
                        dispatchingPaused = false;
                    } else {
                        beginIndex = 0;
                        actionResults = new Object[actions.length];
                    }

                    for (int i = beginIndex; i < actions.length; i++) {
                        ClientAction action = actions[i];
                        Object dispatchResult;
                        try {
                            dispatchingIndex = serverResponse.requestIndex;
                            dispatchResult = action.dispatch(this);
                        } catch (Throwable t) {
                            actionThrowable = t;
                            break;
                        }

                        if (dispatchingPaused) {
                            currentServerResponse = serverResponse;
                            currentActionResults = actionResults;
                            currentActionIndex = i;
                            currentContinueIndex = continueIndex;
                            return;
                        }

                        actionResults[i] = dispatchResult;
                    }
                }

                if (serverResponse.resumeInvocation) {
                    continueIndex++;

                    if (actionThrowable != null) {
                        serverResponse = throwInServerInvocation(serverResponse.requestIndex, continueIndex, ClientExceptionManager.fromDesktopClientToAppServer(actionThrowable));
                    } else {
                        serverResponse = continueServerInvocation(serverResponse.requestIndex, continueIndex, actionResults);
                    }
                    onServerResponse(serverResponse);
                } else {
                    if (actionThrowable != null) {
                        //всегда оборачиваем, чтобы был корректный stack-trace
                        throw new RuntimeException("Exception while dispatching client action: ", actionThrowable);
                    }
                    break;
                }
            } while (true);
        } catch (Exception e) {
            Throwables.propagateIfPossible(e, IOException.class);
        }
    }

    protected abstract RmiQueue getRmiQueue();
    protected abstract RemoteRequestInterface getRemoteRequestInterface();

    protected ServerResponse continueServerInvocation(long requestIndex, final int continueIndex, final Object[] actionResults) throws RemoteException {
        ServerResponse result = getRmiQueue().directRequest(requestIndex, new RmiRequest<ServerResponse>("continueServerInvocation") {
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
                RemoteRequestInterface requestInterface = getRemoteRequestInterface();
                if (requestInterface != null) // for forms, just like in RmiCheckNullFormRequest
                    return requestInterface.continueServerInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, actionResults);
                return null;
            }
        });
        return result == null ? ServerResponse.EMPTY : result;
    }

    protected ServerResponse throwInServerInvocation(long requestIndex, final int continueIndex, final Throwable t) throws IOException {
        ServerResponse result = getRmiQueue().directRequest(requestIndex, new RmiRequest<ServerResponse>("throwInServerInvocation") {
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
                RemoteRequestInterface requestInterface = getRemoteRequestInterface();
                if (requestInterface != null) // for forms, just like in RmiCheckNullFormRequest
                    return requestInterface.throwInServerInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, t);
                return null;
            }
        });
        return result == null ? ServerResponse.EMPTY : result;
    }

    public void pauseDispatching() {
        dispatchingPaused = true;
    }

    public void continueDispatching() {
        continueDispatching(null);
    }

    public void continueDispatching(Object currentActionResult) {
        currentActionResults[currentActionIndex] = currentActionResult;
        try {
            dispatchResponse(currentServerResponse);

            dispatcherListener.dispatchingEnded();
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    //copy from GwtActionDispatcher
    protected long dispatchingIndex = -1;
    public long getDispatchingIndex() {
        if (currentServerResponse == null) // means that we continueDispatching before exiting to dispatchResponse cycle (for example LogicalCellRenderer commits editing immediately)
            return dispatchingIndex;
        else
            return currentServerResponse.requestIndex;
    }

    @Override
    public boolean isDispatchingPaused() {
        return dispatchingPaused;
    }

    public void setEditEvent(EventObject editEvent) {
        this.editEvent = editEvent;
    }

    protected Container getDialogParentContainer() {
        return MainFrame.instance;
    }

    protected abstract PendingRemoteInterface getRemote();

    protected boolean canShowDockedModal() {
        return true;
    }

    protected ShowFormType getShowFormType(FormClientAction action) { // should correspond ClientAsyncOpenForm.isDesktopEnabled
        ShowFormType showFormType = action.showFormType;
        if (showFormType.isDockedModal() && !canShowDockedModal())
            showFormType = ModalityShowFormType.MODAL;
        return showFormType;
    }

    public void execute(FormClientAction action) throws IOException {
        RemoteFormProxy remoteForm = new RemoteFormProxy(action.remoteForm, RemoteObjectProxy.getRealHostName(getRemote()));

        AsyncFormController asyncFormController = getAsyncFormController(getDispatchingIndex());

        ShowFormType showFormType = getShowFormType(action);
        if (showFormType.isWindow()) {
            beforeModalActionInSameEDT(false);
            Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            ClientForm clientForm = ClientFormController.deserializeClientForm(remoteForm, action.clientData);
            ClientModalForm form = new ClientModalForm(owner, clientForm.getCaption(), false) {
                @Override
                public void hideDialog() {
                    super.hideDialog();
                    afterModalActionInSameEDT(false);
                }
            };
            form.init(remoteForm, clientForm, action.clientData, showFormType.isDialog(), editEvent);
            form.showDialog(false);
        } else if (showFormType.isDockedModal()) {
            pauseDispatching();
            beforeModalActionInSameEDT(true);
            ClientFormDockable blockingForm = MainFrame.instance.runForm(asyncFormController, false, remoteForm, action.clientData, openFailed -> {
                afterModalActionInSameEDT(true);
                if (!openFailed) {
                    continueDispatching();
                }
            }, action.formId);
            setBlockingForm(blockingForm);
        } else {
            MainFrame.instance.runForm(asyncFormController, action.forbidDuplicate, remoteForm, action.clientData, null, action.formId);
        }
    }

    protected void beforeModalActionInSameEDT(boolean blockView) {
    }

    protected void setBlockingForm (ClientFormDockable blockingForm) {
    }

    protected void afterModalActionInSameEDT(boolean unblockView) {
    }

    @Override
    public Integer execute(ReportClientAction action) {
        Integer pageCount = null;
        try {
            if (action.autoPrint) {
                ServerPrintAction.autoPrintReport(action.generationData, action.printerName, (e) -> {
                    if (e instanceof JRException && e.getCause() instanceof PrinterAbortException)
                        Log.message(ClientResourceBundle.getString("form.error.print.job.aborted"), false);
                    else
                        ClientExceptionManager.handle(e, false);
                    });
            } else if (action.printType != FormPrintType.PRINT) {
                int editChoice = JOptionPane.NO_OPTION;
                if (action.inDevMode && action.reportPathList.isEmpty()) {
                    editChoice = SwingUtils.showConfirmDialog(MainFrame.instance,
                            getString("layout.menu.file.create.custom.report.choice"),
                            getString("layout.menu.file.create.custom.report.title"),
                            JOptionPane.QUESTION_MESSAGE,
                            false);
                    if (editChoice == JOptionPane.YES_OPTION) {
                        MainController.addReportPathList(action.reportPathList, action.formSID);
                    }
                }
                if (editChoice == JOptionPane.NO_OPTION) {
                    ReportGenerator.exportAndOpen(action.generationData, action.printType, action.sheetName, action.password, action.jasperReportsIgnorePageMargins, MainController.remoteLogics);
                }
            } else {
                if (action.inDevMode) {
                    pageCount = MainFrame.instance.runReport(action.reportPathList, action.formCaption, action.formSID, action.isModal, action.generationData, action.printerName);
                } else {
                    pageCount = MainFrame.instance.runReport(action.isModal, action.formCaption, action.generationData, action.printerName, null);
                }
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return pageCount;
    }

    public Object execute(RuntimeClientAction action) {
        try {
            Process p = Runtime.getRuntime().exec(action.command, action.environment, (action.directory == null ? null : new File(action.directory)));

            if (action.input != null && action.input.length > 0) {
                OutputStream inStream = p.getOutputStream();
                inStream.write(action.input);
            }

            if (action.waitFor) {
                p.waitFor();
            }

            InputStream outStream = p.getInputStream();
            InputStream errStream = p.getErrorStream();

            byte[] output = new byte[outStream.available()];
            outStream.read(output);

            byte[] error = new byte[errStream.available()];
            outStream.read(error);

            return new RuntimeClientActionResult(output, error);

        } catch (IOException | InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    public void execute(ExportFileClientAction action) throws IOException {
        Map<String, RawFileData> chosenFiles = FileDialogUtils.showSaveFileDialog(action.files);
        for(Map.Entry<String, RawFileData> fileEntry : chosenFiles.entrySet()) {
            fileEntry.getValue().write(fileEntry.getKey());
        }
    }

    public Object execute(ImportFileClientAction action) {

        try {

            File file = new File(action.fileName);
            FileInputStream fileStream;

            try {
                fileStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                return new ImportFileClientActionResult(false, "");
            }

            byte[] fileContent = new byte[fileStream.available()];
            fileStream.read(fileContent);
            fileStream.close();

            if (action.erase) {
                BaseUtils.safeDelete(file);
            }

            return new ImportFileClientActionResult(true, action.charsetName == null ? new String(fileContent) : new String(fileContent, action.charsetName));

        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public Object execute(ChooseClassClientAction action) {
        try {
            DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(action.classes));
            ClientObjectClass baseClass = (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inputStream);
            ClientObjectClass defaultClass = (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inputStream);
            ClientObjectClass resultClass = ClassDialog.dialogObjectClass(getDialogParentContainer(), baseClass, defaultClass, action.concrete);
            return resultClass != null ? resultClass.getID() : null;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public Object execute(MessageFileClientAction action) {

        try {

            File file = new File(action.fileName);
            FileInputStream fileStream = null;

            try {
                fileStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                if (action.mustExist) {
                    throw Throwables.propagate(e);
                } else {
                    return null;
                }
            }

            byte[] fileContent = new byte[fileStream.available()];
            fileStream.read(fileContent);
            fileStream.close();

            if (action.erase) {
                BaseUtils.safeDelete(file);
            }

            String fileText = action.charsetName == null ? new String(fileContent) : new String(fileContent, action.charsetName);
            if (action.multiplier > 0) {
                fileText = ((Double) (Double.parseDouble(fileText) * 100)).toString();
            }

            if (action.mask != null) {
                fileText = new DecimalFormat(action.mask).format((Double) (Double.parseDouble(fileText)));
            }

            JOptionPane.showMessageDialog(getDialogParentContainer(), fileText,
                                          action.caption, JOptionPane.INFORMATION_MESSAGE);

            return true;

        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public void execute(UserChangedClientAction action) {
        try {
            MainFrame mainFrame = MainFrame.instance;
            mainFrame.updateUser(MainFrame.getClientSettings(mainFrame.remoteNavigator).currentUserName);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public void execute(MessageClientAction action) {
        boolean log = action.type == MessageClientType.LOG;
        boolean info = action.type == MessageClientType.INFO;
        boolean success = action.type == MessageClientType.SUCCESS;
        boolean warn = action.type == MessageClientType.WARN;
        boolean error = action.type == MessageClientType.ERROR;

        int messageType;
        Color backgroundColor;
        if(info) {
            messageType = JOptionPane.INFORMATION_MESSAGE;
            backgroundColor = Color.decode("#cff4fc");
        } else if(success) {
            messageType = JOptionPane.INFORMATION_MESSAGE;
            backgroundColor = Color.decode("#d1e7dd");
        } else if(warn) {
            messageType = JOptionPane.WARNING_MESSAGE;
            backgroundColor = Color.decode("#fff3cd");
        } else if(error) {
            messageType = JOptionPane.WARNING_MESSAGE;
            backgroundColor = Color.decode("#f8d7da");
        } else { //default
            messageType = JOptionPane.PLAIN_MESSAGE;
            backgroundColor = null;
        }

        if(!log && !info) {
            beforeModalActionInSameEDT(false);
            try {
                if(action.data.isEmpty()) {
                    JOptionPane.showMessageDialog(getDialogParentContainer(),
                            SwingUtils.getMessageTextPane(action.message, backgroundColor), action.caption,
                            messageType);
                } else {
                    Log.messageWarning(action.textMessage, backgroundColor, action.titles, action.data);
                }
            } finally {
                afterModalActionInSameEDT(false);
            }
        }

        if(log || info || error) {
            if (action.data.isEmpty()) {
                Log.message(action.textMessage);
            } else {
                //todo: now only plain text messages in log panel supported
            }
        }
    }

    public int execute(ConfirmClientAction action) {
        beforeModalActionInSameEDT(false);
        try {
            return SwingUtils.showConfirmDialog(getDialogParentContainer(), action.message, action.caption, JOptionPane.QUESTION_MESSAGE,
                    action.cancel, action.timeout, action.initialValue);
        } finally {
            afterModalActionInSameEDT(false);
        }
    }

    public void execute(OpenFileClientAction action) {
        try {
            if (action.file != null) {
                BaseUtils.openFile(action.file, action.name, action.extension);
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public void execute(AudioClientAction action) {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(action.audio));
            clip.open(inputStream);
            clip.start();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void execute(RunEditReportClientAction action) {
    }

    public void execute(HideFormClientAction action) {
    }

    public void execute(ProcessFormChangesClientAction action) {
    }

    public void execute(ProcessNavigatorChangesClientAction action) {
    }

    public Object execute(RequestUserInputClientAction action) {
        throw new UnsupportedOperationException("Request user input action is not supported for this dispatcher");
    }

    public void execute(EditNotPerformedClientAction action) {
        throw new UnsupportedOperationException("EditNotPerformedClientAction not supported for this dispatcher");
    }

    public void execute(UpdateEditValueClientAction action) {
        throw new UnsupportedOperationException("UpdateEditValueClientAction not supported for this dispatcher");
    }

    public void execute(AsyncGetRemoteChangesClientAction action) {
        throw new UnsupportedOperationException("AsyncGetRemoteChangesClientAction not supported for this dispatcher");
    }

    public void execute(LogOutClientAction action) {
        if (action.restart) {
            if (action.reconnect)
                MainController.reconnect();
            else
                MainController.restart();
        } else
            MainController.shutdown();
    }

    public void execute(ExceptionClientAction action) {
        throw Throwables.propagate(action.e);
    }

    @Override
    public String execute(LoadLinkClientAction action) {
        String result = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(SystemUtils.loadCurrentDirectory());
        if(action.directory) {
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        boolean canceled = fileChooser.showOpenDialog(SwingUtils.getActiveWindow()) != JFileChooser.APPROVE_OPTION;
        if (!canceled) {
            File file = fileChooser.getSelectedFile();
            result = file.toURI().toString();
            //replace "file:/" for "file://"
            Pattern p = Pattern.compile("file:/([^/].*)");
            Matcher m = p.matcher(result);
            if(m.matches()) {
                result = "file://" + m.group(1);
            }
            SystemUtils.saveCurrentDirectory(file.getParentFile());
        }
        return result;
    }

    @Override
    public void execute(CopyToClipboardClientAction action) {
        if(action.value != null)
            SwingUtils.copyToClipboard(action.value);
    }

    @Override
    public Map<String, RawFileData> execute(UserLogsClientAction action) {
        Map<String, RawFileData> result = new HashMap<>();
        File logDir = new File(SystemUtils.getUserDir().getAbsolutePath() + "/logs/");
        File[] logFiles = logDir.listFiles();
        if (logFiles != null) {
            for (File logFile : logFiles) {
                try {
                    result.put(logFile.getName(), new RawFileData(logFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    @Override
    public RawFileData execute(ThreadDumpClientAction action) {
        String text = "";
        for(StackTraceElement[] stackTrace : Thread.getAllStackTraces().values())
            text += stackTraceToString(stackTrace) + "\n";
        File file = null;
        try {
            file = File.createTempFile("threaddump", ".txt");
            FileUtils.writeStringToFile(file, text);
            return new RawFileData(file);
        } catch (IOException e) {
            return null;
        } finally {
            BaseUtils.safeDelete(file);
        }
    }

    private String stackTraceToString(StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public void execute(final BeepClientAction action) {
        if (action.async) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    beep(action.file);
                }
            }).run();
        } else {
            beep(action.file);
        }
    }

    private void beep(RawFileData rawFile) {
        File file = null;
        try {
            //support only .wav files
            file = File.createTempFile("beep", getExtension(rawFile.getBytes()));
            rawFile.write(file);

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(file.toURI().toURL());
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            BaseUtils.safeDelete(file);
        }
    }

    @Override
    public void execute(ActivateFormClientAction action) {
        ((DockableMainFrame) MainFrame.instance).activateForm(action.formCanonicalName);
    }

    @Override
    public void execute(MaximizeFormClientAction action) {
        ((DockableMainFrame) MainFrame.instance).maximizeForm();
    }

    @Override
    public void execute(CloseFormClientAction action) {
        ((DockableMainFrame) MainFrame.instance).closeForm(action.formId);
    }

    @Override
    public void execute(ChangeColorThemeClientAction action) {
        MainController.changeColorTheme(action.colorTheme);
    }

    @Override
    public void execute(ResetWindowsLayoutClientAction action) {
        ((DockableMainFrame) MainFrame.instance).resetWindowsLayout();
    }

    @Override
    public void execute(ClientWebAction action) {
        if (action.isFont()) {
            SystemUtils.registerFont(action);
        } else if(action.isLibrary()) {
            SystemUtils.registerLibrary(action);
        }
    }

    @Override
    public void execute(OrderClientAction action) {
    }

    @Override
    public void execute(FilterClientAction action) {
    }

    @Override
    public void execute(FilterGroupClientAction action) {
    }

    @Override
    public EventBus getEventBus() {
        return MainFrame.instance.eventBus;
    }

    @Override
    public void addCleanListener(ICleanListener daemonTask) {
        MainFrame.instance.cleanListeners.add(daemonTask);
    }

    private String getExtension(byte[] file) {
        return file[0] == 73 && file[1] == 68 && file[2] == 51 ? ".mp3" : ".wav";
    }

    public AsyncFormController getAsyncFormController(long requestIndex) {
        return getRmiQueue().getAsyncFormController(requestIndex);
    }
}
