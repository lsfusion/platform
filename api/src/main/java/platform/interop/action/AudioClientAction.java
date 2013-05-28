package platform.interop.action;

import platform.base.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class AudioClientAction extends ExecuteClientAction {

    public byte[] audio;

    public AudioClientAction(InputStream in) throws IOException {
        this(IOUtils.readBytesFromStream(in));
    }

    public AudioClientAction(byte[] audio) {
        this.audio = audio;
    }

    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
