package platform.server.view.form.client;

import platform.server.view.form.client.decorator.Highlighter;

import java.io.DataOutputStream;
import java.io.IOException;

public class GridView extends ComponentView implements ClientSerialize {

    public boolean showFind = false;
    public boolean showFilter = true;

    public byte minRowCount = 0;
    public boolean tabVertical = false;
    public Highlighter highlighter;

    public GridView(int ID) {
        super(ID);
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(showFind);
        outStream.writeBoolean(showFilter);

        outStream.writeByte(minRowCount);
        outStream.writeBoolean(tabVertical);

        outStream.writeBoolean(highlighter == null);
        if (highlighter != null) {
            highlighter.serialize(outStream);
        }
    }
}
