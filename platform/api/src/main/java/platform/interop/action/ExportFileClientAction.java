package platform.interop.action;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

public class ExportFileClientAction extends AbstractClientAction {

    public Map<String, byte[]> files;

    public ExportFileClientAction(String fileName, String fileText, String charsetName) {
        try {
            if (charsetName != null) {
                files.put(fileName, fileText.getBytes(charsetName));
            } else {
                files.put(fileName, fileText.getBytes());
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public ExportFileClientAction(Map<String, byte[]> files) {
        this.files = files;
    }

    @Override
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
