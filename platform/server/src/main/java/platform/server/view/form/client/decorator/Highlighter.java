package platform.server.view.form.client.decorator;

import platform.server.view.form.client.ClientSerialize;
import platform.server.view.form.client.PropertyCellView;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class Highlighter implements ClientSerialize {
    private Color color;
    private PropertyCellView cellView;

    public Highlighter(Color color, PropertyCellView cellView) {
        this.color = color;
        this.cellView = cellView;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeUTF(cellView.getSID());
        new ObjectOutputStream(outStream).writeObject(color);
    }
}
