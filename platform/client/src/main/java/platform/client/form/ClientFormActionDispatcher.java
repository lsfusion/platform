package platform.client.form;

import platform.client.Main;
import platform.client.Log;
import platform.client.navigator.ClientNavigator;
import platform.interop.action.*;

import javax.swing.*;
import java.io.*;

public class ClientFormActionDispatcher implements ClientActionDispatcher {

    ClientNavigator clientNavigator;

    public ClientFormActionDispatcher(ClientNavigator clientNavigator) {
        this.clientNavigator = clientNavigator;
    }

    public Object execute(FormClientAction action) {
        try {
            if (action.isPrintForm)
                Main.frame.runReport(clientNavigator, action.remoteForm);
            else
                Main.frame.runForm(clientNavigator, action.remoteForm);
            return true;
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

    public Object execute(ExportFileClientAction action) {

        try {

            FileOutputStream output = new FileOutputStream(action.fileName);
            if (action.charsetName == null)
                output.write(action.fileText.getBytes());
            else
                output.write(action.fileText.getBytes(action.charsetName));
            output.close();

            return true;
            
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

    public Object execute(SleepClientAction action) {
        
        try {
            Thread.sleep(action.millis);
            return true;
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
                fileText = ((Double)(Double.parseDouble(fileText) * 100)).toString();
            }

            JOptionPane.showMessageDialog(null, fileText,
                                          action.caption, JOptionPane.INFORMATION_MESSAGE);

            return true;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object execute(UserChangedClientAction action) {
        try {
            Main.frame.drawCurrentUser(clientNavigator.remoteNavigator);

            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Object execute(MessageClientAction action) {

        JOptionPane.showMessageDialog(null, action.message,
                                      action.caption, JOptionPane.INFORMATION_MESSAGE);

        return true;
    }

    public Object execute(ResultClientAction action) {
        if(action.failed) {
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
}
