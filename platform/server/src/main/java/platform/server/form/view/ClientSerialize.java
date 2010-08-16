package platform.server.form.view;

import java.io.DataOutputStream;
import java.io.IOException;

public interface ClientSerialize {

    void serialize(DataOutputStream outStream) throws IOException;
}
