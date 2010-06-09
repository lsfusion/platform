package platform.server.view.form.client;

import java.io.DataOutputStream;
import java.io.IOException;

public class GridView extends ComponentView implements ClientSerialize {

    public boolean showFind = false;
    public boolean showFilter = true;

    public GridView(int ID) {
        super(ID);
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(showFind);
        outStream.writeBoolean(showFilter);
    }
}
