package lsfusion.client.form.dispatch;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.SystemUtils;
import lsfusion.client.Log;
import lsfusion.client.Main;
import lsfusion.client.MainFrame;
import lsfusion.client.SwingUtils;
import lsfusion.client.form.ClientDialog;
import lsfusion.client.form.ClientModalForm;
import lsfusion.client.form.ClientNavigatorDialog;
import lsfusion.client.form.classes.ClassDialog;
import lsfusion.client.logics.classes.ClientObjectClass;
import lsfusion.client.logics.classes.ClientTypeSerializer;
import lsfusion.client.remote.proxy.RemoteDialogProxy;
import lsfusion.client.remote.proxy.RemoteFormProxy;
import lsfusion.interop.KeyStrokes;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.*;
import lsfusion.interop.form.RemoteDialogInterface;
import lsfusion.interop.form.ServerResponse;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;

public abstract class SwingClientActionDispatcher implements ClientActionDispatcher {
    private boolean dispatchingPaused;

    private ServerResponse currentServerResponse = null;
    Object[] currentActionResults = null;
    private int currentActionIndex = -1;

    public void dispatchResponse(ServerResponse serverResponse) throws IOException {
        assert serverResponse != null;

        try {
            preDispatchResponse(serverResponse);
            do {
                Object[] actionResults = null;
                ClientAction[] actions = serverResponse.actions;
                if (actions != null) {
                    int beginIndex;
                    if (dispatchingPaused) {
                        beginIndex = currentActionIndex + 1;
                        actionResults = currentActionResults;

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
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            throwInServerInvocation(ex);
                            break;
                        }

                        if (dispatchingPaused) {
                            currentServerResponse = serverResponse;
                            currentActionResults = actionResults;
                            currentActionIndex = i;
                            return;
                        }

                        actionResults[i] = dispatchResult;
                    }
                }

                if (!serverResponse.resumeInvocation) {
                    break;
                }

                serverResponse = continueServerInvocation(actionResults);
            } while (true);

            postDispatchResponse(serverResponse);
        } catch (Exception e) {
            handleDispatchException(e);
        }
    }

    protected void preDispatchResponse(ServerResponse serverResponse) throws IOException {
    }

    protected void postDispatchResponse(ServerResponse serverResponse) throws IOException {
        assert !serverResponse.resumeInvocation;
    }

    protected void handleDispatchException(Exception e) throws IOException {
        Throwables.propagateIfPossible(e, IOException.class);
    }

    protected abstract void throwInServerInvocation(Exception ex) throws IOException;
    protected abstract ServerResponse continueServerInvocation(Object[] actionResults) throws RemoteException;

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
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    protected Container getDialogParentContainer() {
        return Main.frame;
    }

    public void execute(FormClientAction action) {
        RemoteFormProxy remoteForm = new RemoteFormProxy(action.remoteForm);
        if (action.modalityType == ModalityType.DOCKED_MODAL) {
            pauseDispatching();
            beforeShowDockedModalForm();
            Main.frame.runForm(remoteForm, new MainFrame.FormCloseListener() {
                @Override
                public void formClosed() {
                    afterHideDockedModalForm();
                    continueDispatching();
                }
            });
        } else if (action.modalityType.isModal()) {
            new ClientModalForm(Main.frame, remoteForm).showDialog(action.modalityType.isFullScreen());
        } else {
            Main.frame.runForm(remoteForm, null);
        }
    }

    protected void beforeShowDockedModalForm() {
    }

    protected void afterHideDockedModalForm() {
    }

    public void execute(ReportClientAction action) {
    }

    public void execute(DialogClientAction action) {
        AWTEvent currentEvent = EventQueue.getCurrentEvent();

        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

        RemoteDialogInterface dialog = new RemoteDialogProxy(action.dialog);

        ClientDialog dlg;
        if (KeyStrokes.isSpaceEvent(currentEvent) || KeyStrokes.isObjectEditorDialogEvent(currentEvent)) {
            dlg = new ClientNavigatorDialog(owner, dialog);
        } else {
            dlg = new ClientDialog(owner, dialog, currentEvent);
        }

        dlg.showDialog(false);
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

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(ExportFileClientAction action) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(SystemUtils.loadCurrentDirectory());
            boolean singleFile;
            if (action.files.size() > 1) {
                singleFile = false;
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setAcceptAllFileFilterUsed(false);
            } else {
                singleFile = true;
                fileChooser.setSelectedFile(new File(action.files.keySet().iterator().next()));
            }
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                for (String file : action.files.keySet()) {
                    IOUtils.putFileBytes(new File(singleFile ? path : path + "\\" + file), action.files.get(file));
                }
                SystemUtils.saveCurrentDirectory(!singleFile ? new File(path) : new File(path.substring(0, path.lastIndexOf("\\"))));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
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
        if (!action.extended) {
            JOptionPane.showMessageDialog(getDialogParentContainer(), action.message, action.caption, JOptionPane.INFORMATION_MESSAGE);
        } else {
            new ExtendedMessageDialog(getDialogParentContainer(), action.caption, action.message).setVisible(true);
        }
    }

    public int execute(ConfirmClientAction action) {
        return SwingUtils.showConfirmDialog(getDialogParentContainer(), action.message, action.caption, JOptionPane.QUESTION_MESSAGE);
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
            Log.error(action.message, action.titles, action.data);
        } else {
            Log.message(action.message);
        }
    }

    public void execute(OpenFileClientAction action) {
        try {
            if (action.file != null) {
                BaseUtils.openFile(action.file, action.extension);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public void execute(RunPrintReportClientAction action) {
    }

    public void execute(RunOpenInExcelClientAction action) {
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
        Main.restart();
    }
}
