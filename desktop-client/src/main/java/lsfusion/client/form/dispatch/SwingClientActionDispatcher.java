package lsfusion.client.form.dispatch;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.SystemUtils;
import lsfusion.client.Log;
import lsfusion.client.Main;
import lsfusion.client.MainFrame;
import lsfusion.client.SwingUtils;
import lsfusion.client.form.ClientModalForm;
import lsfusion.client.form.classes.ClassDialog;
import lsfusion.client.logics.classes.ClientObjectClass;
import lsfusion.client.logics.classes.ClientTypeSerializer;
import lsfusion.client.remote.proxy.RemoteFormProxy;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.*;
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
import java.util.EventObject;

public abstract class SwingClientActionDispatcher implements ClientActionDispatcher {
    private EventObject editEvent;

    private boolean dispatchingPaused;

    private ServerResponse currentServerResponse = null;
    Object[] currentActionResults = null;
    private int currentActionIndex = -1;
    private int currentContinueIndex = -1;

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
        } catch (IOException e) {
            Throwables.propagate(e);
        }
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
            Main.frame.runForm(action.canonicalName, action.formSID, remoteForm, action.firstChanges, new MainFrame.FormCloseListener() {
                @Override
                public void formClosed() {
                    afterModalActionInSameEDT(true);
                    continueDispatching();
                }
            });
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
            Main.frame.runForm(action.canonicalName, action.formSID, remoteForm, action.firstChanges, null);
        }
    }

    protected void beforeModalActionInSameEDT(boolean blockView) {
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
        beforeModalActionInSameEDT(false);
        try {
            if (!action.extended) {
                JOptionPane.showMessageDialog(getDialogParentContainer(), action.message, action.caption, JOptionPane.INFORMATION_MESSAGE);
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
            Log.error(action.message, action.titles, action.data, true);
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

    @Override
    public void execute(FocusClientAction action) {
        //do nothing by default
    }
}
