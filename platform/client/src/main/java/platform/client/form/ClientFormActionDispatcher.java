package platform.client.form;

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.client.Log;
import platform.client.Main;
import platform.interop.action.*;
import platform.interop.exceptions.LoginException;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
            if (!action.isModal) {
                if (action.isPrintForm)
                    Main.frame.runReport(action.remoteForm);
                else
                    Main.frame.runForm(action.remoteForm);
            } else {
                ClientModalForm modalForm = new ClientModalForm(Main.frame, action.remoteForm, action.newSession);
                modalForm.setDefaultSize();
                modalForm.setLocationRelativeTo(null);
                modalForm.setVisible(true);
                modalForm.dispose();
                modalForm.closed();
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
            String path = "";
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                path = fileChooser.getSelectedFile().getAbsolutePath();
                preferences.put("LATEST_DIRECTORY", path);
            }
            for (String file : action.files.keySet()) {
                IOUtils.putFileBytes(new File(path + "\\" + file), action.files.get(file));
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

            JOptionPane.showMessageDialog(null, fileText,
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
            JDialog dialog = jop.createDialog("Введите пароль");
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

        JOptionPane.showMessageDialog(null, action.message,
                action.caption, JOptionPane.INFORMATION_MESSAGE);
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
}
