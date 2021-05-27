package lsfusion.client.controller.dispatch;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.base.file.FileDialogUtils;
import lsfusion.base.file.RawFileData;
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
import lsfusion.client.form.print.ClientReportUtils;
import lsfusion.client.form.view.ClientFormDockable;
import lsfusion.client.form.view.ClientModalForm;
import lsfusion.client.view.DockableMainFrame;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.action.*;
import lsfusion.interop.base.remote.PendingRemoteInterface;
import lsfusion.interop.base.remote.RemoteRequestInterface;
import lsfusion.interop.form.ModalityType;
import lsfusion.interop.form.event.EventBus;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerator;
import org.apache.commons.io.FileUtils;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

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

    public void execute(FormClientAction action) {
        RemoteFormProxy remoteForm = new RemoteFormProxy(action.remoteForm, RemoteObjectProxy.getRealHostName(getRemote()));
        if(action.immutableMethods != null) {
            for (int i = 0; i < FormClientAction.methodNames.length; i++) {
                remoteForm.setProperty(FormClientAction.methodNames[i], action.immutableMethods[i]);
            }
        }

        ModalityType modalityType = action.modalityType;
        if (modalityType == ModalityType.DOCKED_MODAL) {
            pauseDispatching();
            beforeModalActionInSameEDT(true);
            ClientFormDockable blockingForm = MainFrame.instance.runForm(getDispatchingIndex(), action.canonicalName, action.formSID, false, remoteForm, action.firstChanges, openFailed -> {
                afterModalActionInSameEDT(true);
                if (!openFailed) {
                    continueDispatching();
                }
            });
            setBlockingForm(blockingForm);
        } else if (modalityType.isModal()) {
            beforeModalActionInSameEDT(false);
            Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            ClientForm clientForm = ClientFormController.deserializeClientForm(remoteForm);
            ClientModalForm form = new ClientModalForm(owner, clientForm.getCaption(), modalityType, false) {
                @Override
                public void hideDialog() {
                    super.hideDialog();
                    afterModalActionInSameEDT(false);
                }
            };
            form.init(action.canonicalName, action.formSID, remoteForm, clientForm, action.firstChanges, modalityType.isDialog(), editEvent);
            form.showDialog(false);
        } else {
            MainFrame.instance.runForm(getDispatchingIndex(), action.canonicalName, action.formSID, action.forbidDuplicate, remoteForm, action.firstChanges, null);
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
            if (action.printType == FormPrintType.AUTO) {
                ClientReportUtils.autoprintReport(action.generationData, action.printerName);
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
                    ReportGenerator.exportAndOpen(action.generationData, action.printType, action.sheetName, action.password, MainController.remoteLogics);
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
                file.delete();
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
                file.delete();
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
            MainFrame.instance.updateUser(MainFrame.instance.remoteNavigator.getClientSettings().currentUserName);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public void execute(MessageClientAction action) {
        beforeModalActionInSameEDT(false);
        try {
            if (!action.extended) {
                JTextPane textPane = SwingUtils.getMessageTextPane(action.message);
                JOptionPane.showMessageDialog(getDialogParentContainer(), textPane, action.caption, JOptionPane.INFORMATION_MESSAGE);
            } else {
                new ExtendedMessageDialog(getDialogParentContainer(), action.caption, action.message).setVisible(true);
            }
        } finally {
            afterModalActionInSameEDT(false);
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

    public class ExtendedMessageDialog extends JDialog {
        public ExtendedMessageDialog(Container owner, String title, String message) {
            super(SwingUtils.getWindow(owner), title, ModalityType.DOCUMENT_MODAL);
            JTextArea textArea = new JTextArea(message);
            textArea.setFont(textArea.getFont().deriveFont((float) 12));
            add(new JScrollPane(textArea));
            setMinimumSize(new Dimension(400, 200));
            setLocationRelativeTo(owner);
            ActionListener escListener = new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    setVisible(false);
                }
            };
            KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            getRootPane().registerKeyboardAction(escListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
    }

    public void execute(LogMessageClientAction action) {
        if (action.failed) {
            Log.messageWarning(action.message, action.titles, action.data);
        } else {
            Log.message(action.message);
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
            if(result.startsWith("file:/")) {
                result = result.replaceFirst("file:/", "file://");
            }
            SystemUtils.saveCurrentDirectory(file.getParentFile());
        }
        return result;
    }

    @Override
    public boolean execute(CopyToClipboardClientAction action) {
        if(action.value != null)
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(action.value), null);
        return true;
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
            if(file != null && !file.delete())
                file.deleteOnExit();
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
            if (file != null && !file.delete())
                file.deleteOnExit();
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
    public void execute(ChangeColorThemeClientAction action) {
    }

    @Override
    public void execute(ResetWindowsLayoutClientAction action) {
        ((DockableMainFrame) MainFrame.instance).resetWindowsLayout();
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
}
