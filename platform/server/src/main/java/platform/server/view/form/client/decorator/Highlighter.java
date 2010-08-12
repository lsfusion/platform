package platform.server.view.form.client.decorator;

import platform.server.view.form.client.ClientSerialize;
import platform.server.view.form.client.PropertyCellView;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 *  Example of use:
 *
 *      public DefaultFormView createDefaultRichDesign() {
 *          DefaultFormView design = super.createDefaultRichDesign();
 *
 *          PropertyViewNavigator propertyView = getPropertyView(someLP);
 *          PropertyCellView cellView = design.get(propertyView);
 *
 *          design.get(propertyView.toDraw).gridView.highlighter = new Highlighter(Color.yellow, cellView);
 *
 *          return design;
 *      }
 */
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
