package lsfusion.utils.admin.interpreter;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.BufferedInputStream;
import java.io.IOException;


public class RunCommandClientAction implements ClientAction {

    String text;

    public RunCommandClientAction(String text) {
        this.text = text;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return text != null ? exec(text) : null;
    }

    private String exec(String command) {
        try {
            Process p = Runtime.getRuntime().exec(command);
            BufferedInputStream err = new BufferedInputStream(p.getErrorStream());
            StringBuilder errS = new StringBuilder();
            byte[] b = new byte[1024];
            while (err.read(b) != -1) {
                errS.append(new String(b, "cp866").trim()).append("\n");
            }
            err.close();
            String result = errS.toString();
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}