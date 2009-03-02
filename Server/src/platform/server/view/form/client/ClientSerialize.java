package platform.server.view.form.client;

import java.io.DataOutputStream;
import java.io.IOException;

public interface ClientSerialize {

    void serialize(DataOutputStream outStream) throws IOException;
}
