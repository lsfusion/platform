package platform.client.form;

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.client.ClientResourceBundle;
import platform.client.Log;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.remote.proxy.RemoteFormProxy;
import platform.interop.action.*;
import platform.interop.exceptions.LoginException;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.prefs.Preferences;

public class ClientFormActionDispatcher implements ClientActionDispatcher {

    private final ClientFormController form;
    public ClientFormActionDispatcher(ClientFormController form) {
        this.form = form;
    }

    public void execute(FormClientAction action) {
        try {
            RemoteFormProxy remoteForm = new RemoteFormProxy(action.remoteForm);
            if (action.isPrintForm) {
                Main.frame.runReport(remoteForm, action.isModal);
            } else {
                if (!action.isModal) {
                    Main.frame.runForm(remoteForm);
                } else {
                    new ClientModalForm(Main.frame, remoteForm, action.newSession).showDialog(false);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object execute(RuntimeClientAction action) {

        try {

            Process p = Runtime.getRuntime().exec(action.command, action.environment, (action.directory == null ? null : new File(action.directory)));

            if (action.input != null && action.input.length > 0) {
                OutputStream inStream = p.getOutputStream();
                inStream.write(action.input);
            }

            if (action.waitFor)
                p.waitFor();

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
            Preferences preferences = Preferences.userNodeForPackage(this.getClass());
            fileChooser.setCurrentDirectory(new File(preferences.get("LATEST_DIRECTORY", "")));
            boolean singleFile;
            if (action.files.size() > 1) {
                singleFile = false;
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setAcceptAllFileFilterUsed(false);
            } else {
                singleFile = true;
            }
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                for (String file : action.files.keySet()) {
                    IOUtils.putFileBytes(new File(singleFile ? path : path + "\\" + file), action.files.get(file));
                }
                preferences.put("LATEST_DIRECTORY", !singleFile ? path : path.substring(0, path.lastIndexOf("\\")));
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

            if (action.erase) file.delete();

            return new ImportFileClientActionResult(true, action.charsetName == null ? new String(fileContent) : new String(fileContent, action.charsetName));

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(SleepClientAction action) {

        try {
            Thread.sleep(action.millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Object execute(MessageFileClientAction action) {

        try {

            File file = new File(action.fileName);
            FileInputStream fileStream = null;

            try {
                fileStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                if (action.mustExist)
                    throw new RuntimeException(e);
                else
                    return null;
            }

            byte[] fileContent = new byte[fileStream.available()];
            fileStream.read(fileContent);
            fileStream.close();

            if (action.erase) file.delete();

            String fileText = action.charsetName == null ? new String(fileContent) : new String(fileContent, action.charsetName);
            if (action.multiplier > 0) {
                fileText = ((Double) (Double.parseDouble(fileText) * 100)).toString();
            }

            if (action.mask != null) {
                fileText = new DecimalFormat(action.mask).format((Double) (Double.parseDouble(fileText))); 
            }

            JOptionPane.showMessageDialog(form.getComponent(), fileText,
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
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(UserReloginClientAction action) {
        try {
            final JPasswordField jpf = new JPasswordField();
            JOptionPane jop = new JOptionPane(jpf,
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
            JDialog dialog = jop.createDialog(form.getComponent(), ClientResourceBundle.getString("form.enter.password"));
            dialog.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    jpf.requestFocusInWindow();
                }
            });
            dialog.setVisible(true);
            int result = (jop.getValue() != null) ? (Integer) jop.getValue() : JOptionPane.CANCEL_OPTION;
            dialog.dispose();
            String password = null;
            if (result == JOptionPane.OK_OPTION) {
                password = new String(jpf.getPassword());
                boolean check = Main.remoteLogics.checkUser(action.login, password);
                if (check) {
                    Main.frame.remoteNavigator.relogin(action.login);
                    Main.frame.updateUser();
                } else
                    throw new RuntimeException();
            }
        } catch (LoginException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(MessageClientAction action) {
        if (!action.extended) {
            JOptionPane.showMessageDialog(form.getComponent(), action.message, action.caption, JOptionPane.INFORMATION_MESSAGE);
        } else {
            new ExtendedMessageDialog(form.getComponent(), action.caption, action.message).setVisible(true);
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

    public Object execute(ResultClientAction action) {
        if (action.failed) {
            Log.printFailedMessage(action.message);
            return null;
        } else {
            Log.printSuccessMessage(action.message);
            return true;
        }
    }

    public Object execute(CustomClientAction action) {
        return action.execute();
    }

    public void execute(ApplyClientAction action) {
        form.applyChanges(true);
    }

    public void execute(OpenFileClientAction action) {
        try {
            if(action.file!=null)
                BaseUtils.openFile(action.file, action.extension);
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
}
