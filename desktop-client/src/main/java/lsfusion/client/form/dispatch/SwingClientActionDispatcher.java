package lsfusion.client.form.dispatch;

import com.google.common.base.Throwables;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import jssc.SerialPort;
import jssc.SerialPortException;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.SystemUtils;
import lsfusion.client.Log;
import lsfusion.client.Main;
import lsfusion.client.MainFrame;
import lsfusion.client.SwingUtils;
import lsfusion.client.dock.ClientFormDockable;
import lsfusion.client.dock.DockableMainFrame;
import lsfusion.client.form.ClientModalForm;
import lsfusion.client.form.DispatcherListener;
import lsfusion.client.form.classes.ClassDialog;
import lsfusion.client.logics.classes.ClientObjectClass;
import lsfusion.client.logics.classes.ClientTypeSerializer;
import lsfusion.client.remote.proxy.RemoteFormProxy;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.*;
import lsfusion.interop.form.ServerResponse;
import org.apache.commons.io.FileUtils;

import javax.sound.sampled.*;
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
                        serverResponse = throwInServerInvocation(serverResponse.requestIndex, continueIndex, actionThrowable);
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

    protected abstract ServerResponse throwInServerInvocation(long requestIndex, int continueIndex, Throwable t) throws IOException;

    protected abstract ServerResponse continueServerInvocation(long requestIndex, int continueIndex, Object[] actionResults) throws RemoteException;

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

    @Override
    public boolean isDispatchingPaused() {
        return dispatchingPaused;
    }

    public void setEditEvent(EventObject editEvent) {
        this.editEvent = editEvent;
    }

    protected Container getDialogParentContainer() {
        return Main.frame;
    }

    public void execute(FormClientAction action) {
        RemoteFormProxy remoteForm = new RemoteFormProxy(action.remoteForm);
        if(action.immutableMethods != null) {
            for (int i = 0; i < FormClientAction.methodNames.length; i++) {
                remoteForm.setProperty(FormClientAction.methodNames[i], action.immutableMethods[i]);
            }
        }

        ModalityType modality = action.modalityType;
        if (modality == ModalityType.DOCKED_MODAL) {
            pauseDispatching();
            beforeModalActionInSameEDT(true);
            ClientFormDockable blockingForm = Main.frame.runForm(action.canonicalName, action.formSID, false, remoteForm, action.firstChanges, new MainFrame.FormCloseListener() {
                @Override
                public void formClosed() {
                    afterModalActionInSameEDT(true);
                    continueDispatching();
                }
            });
            setBlockingForm(blockingForm);
        } else if (modality.isModal()) {
            beforeModalActionInSameEDT(false);
            Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            new ClientModalForm(action.canonicalName, action.formSID, owner, remoteForm, action.firstChanges, modality.isDialog(), editEvent) {
                @Override
                public void hideDialog() {
                    super.hideDialog();
                    afterModalActionInSameEDT(false);
                }
            }.showDialog(modality.isFullScreen());
        } else {
            Main.frame.runForm(action.canonicalName, action.formSID, action.forbidDuplicate, remoteForm, action.firstChanges, null);
        }
    }

    protected void beforeModalActionInSameEDT(boolean blockView) {
    }

    protected void setBlockingForm (ClientFormDockable blockingForm) {
    }

    protected void afterModalActionInSameEDT(boolean unblockView) {
    }

    public Integer execute(ReportClientAction action) {
        return null;
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
            throw new RuntimeException(e);
        }
    }

    public void execute(ExportFileClientAction action) {
        SwingUtils.showSaveFileDialog(action.files);
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
            throw new RuntimeException(e);
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
                    throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
    }

    public void execute(UserChangedClientAction action) {
        try {
            Main.frame.updateUser();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(MessageClientAction action) {
        beforeModalActionInSameEDT(false);
        try {
            if (!action.extended) {
                JTextPane textPane = new JTextPane();
                textPane.setText(String.valueOf(action.message)); //message can be null
                textPane.setEditable(false);
                int width = (int) (Main.frame.getRootPane().getWidth() * 0.3);
                textPane.setSize(new Dimension(width, 10));
                if(getWidth(action.message) >= width) { //set preferred size only for text with long lines
                    int height = Math.min((int) (Main.frame.getRootPane().getHeight() * 0.9), textPane.getPreferredSize().height);
                    textPane.setPreferredSize((new Dimension(width, height)));
                }
                textPane.setBackground(null);
                JOptionPane.showMessageDialog(getDialogParentContainer(), textPane, action.caption, JOptionPane.INFORMATION_MESSAGE);
            } else {
                new ExtendedMessageDialog(getDialogParentContainer(), action.caption, action.message).setVisible(true);
            }
        } finally {
            afterModalActionInSameEDT(false);
        }
    }

    private int getWidth(String message) {
        try {
            FontMetrics metrics = getDialogParentContainer().getGraphics().getFontMetrics();
            int maxWidth = 0;
            if (metrics != null) {
                for (String line : message.split("\n")) {
                    int width = metrics.stringWidth(line);
                    if (width > maxWidth)
                        maxWidth = width;
                }
            }
            return maxWidth;
        } catch (Exception e) {
            return 0;
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
            throw new RuntimeException(e);
        }
    }

    public void execute(final SaveFileClientAction action) {
        SwingUtils.showSaveFileDialog(action.getFileMap(), action.noDialog, action.append);
    }

    public void execute(AudioClientAction action) {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(action.audio));
            clip.open(inputStream);
            clip.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
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
                Main.reconnect();
            else
                Main.restart();
        } else
            Main.shutdown();
    }

    public void execute(ExceptionClientAction action) {
        throw new RuntimeException(action.e);
    }

    @Override
    public String execute(LoadLinkClientAction action) {
        String result = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(SystemUtils.loadCurrentDirectory());
        boolean canceled = fileChooser.showOpenDialog(SwingUtils.getActiveWindow()) != JFileChooser.APPROVE_OPTION;
        if (!canceled) {
            File file = fileChooser.getSelectedFile();
            result = file.toURI().toString();
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
    public Map<String, byte[]> execute(UserLogsClientAction action) {
        Map<String, byte[]> result = new HashMap<>();
        File logDir = new File(SystemUtils.getUserDir().getAbsolutePath() + "/logs/");
        File[] logFiles = logDir.listFiles();
        if (logFiles != null) {
            for (File logFile : logFiles) {
                try {
                    result.put(logFile.getName(), IOUtils.getFileBytes(logFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    @Override
    public byte[] execute(ThreadDumpClientAction action) {
        String text = "";
        for(StackTraceElement[] stackTrace : Thread.getAllStackTraces().values())
            text += stackTraceToString(stackTrace) + "\n";
        File file = null;
        try {
            file = File.createTempFile("threaddump", ".txt");
            FileUtils.writeStringToFile(file, text);
            return IOUtils.getFileBytes(file);
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

    private void beep(byte[] fileBytes) {
        File file = null;
        try {
            file = File.createTempFile("beep", getExtension(fileBytes));
            FileUtils.writeByteArrayToFile(file, fileBytes);
            Media hit = new Media(file.toURI().toString());
            createJFXPanel();
            MediaPlayer mediaPlayer = new MediaPlayer(hit);
            mediaPlayer.play();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            if (file != null && !file.delete())
                file.deleteOnExit();
        }
    }

    @Override
    public void execute(ActivateFormClientAction action) {
        ((DockableMainFrame) Main.frame).activateForm(action.formCanonicalName);
    }

    @Override
    public void execute(MaximizeFormClientAction action) {
        ((DockableMainFrame) Main.frame).maximizeForm();
    }

    @Override
    public String execute(WriteToComPortClientAction action) {
        if(action.daemon) {
            return Main.writeToComPort(action.file, action.comPort);
        } else {
            try {
                SerialPort serialPort = new SerialPort("COM" + action.comPort);
                try {
                    serialPort.openPort();
                    serialPort.setParams(action.baudRate, 8, 1, 0);
                    serialPort.writeBytes(action.file);
                } finally {
                    serialPort.closePort();
                }
            } catch (SerialPortException e) {
                return e.getMessage();
            }
            return null;
        }
    }

    //to prevent java.lang.IllegalStateException: Toolkit not initialized
    // https://rterp.wordpress.com/2015/04/04/javafx-toolkit-not-initialized-solved/
    private void createJFXPanel() {
        new JFXPanel();
    }

    private String getExtension(byte[] file) {
        return file[0] == 73 && file[1] == 68 && file[2] == 51 ? ".mp3" : ".wav";
    }
}
