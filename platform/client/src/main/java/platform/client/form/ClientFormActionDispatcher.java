package platform.client.form;

import platform.interop.action.*;
import platform.client.navigator.ClientNavigator;
import platform.client.Main;
import platform.client.layout.ReportDockable;
import platform.client.layout.ClientFormDockable;

import java.io.*;
import java.nio.CharBuffer;
import java.util.Scanner;

public class ClientFormActionDispatcher implements ClientActionDispatcher {

    ClientNavigator clientNavigator;

    public ClientFormActionDispatcher(ClientNavigator clientNavigator) {
        this.clientNavigator = clientNavigator;
    }

    public ClientActionResult executeForm(FormClientAction action) {
        try {
            Main.layout.defaultStation.drop(action.isPrintForm?new ReportDockable(clientNavigator, action.remoteForm):new ClientFormDockable(clientNavigator, action.remoteForm));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public RuntimeClientActionResult executeRuntime(RuntimeClientAction action) {
        
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

    public ClientActionResult executeExportFile(ExportFileClientAction action) {

        try {

            FileWriter writer = new FileWriter(action.fileName);
            writer.append(action.fileText);
            writer.close();

            return null;
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ImportFileClientActionResult executeImportFile(ImportFileClientAction action) {

        try {

            FileInputStream fileStream;
            try {
                fileStream = new FileInputStream(new File(action.fileName));
            } catch (FileNotFoundException e) {
                return new ImportFileClientActionResult(false, "");
            }

            byte[] fileContent = new byte[fileStream.available()];
            fileStream.read(fileContent);

            return new ImportFileClientActionResult(true, new String(fileContent));

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
