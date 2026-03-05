package lsfusion.base.net;

import com.google.common.base.Throwables;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class WriteToSocketClientAction extends ExecuteClientAction {
    public final String text;
    public final String charset;
    public final String ip;
    public final Integer port;

    public WriteToSocketClientAction(String text, String charset, String ip, Integer port) {
        this.text = text;
        this.charset = charset;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        try (OutputStream os = new Socket(ip, port).getOutputStream()) {
            os.write(text.getBytes(charset));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
