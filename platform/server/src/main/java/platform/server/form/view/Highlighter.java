package platform.server.form.view;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 *  Example of use:
 *  <pre><code>
 *      public DefaultFormView createDefaultRichDesign() {
 *          DefaultFormView design = super.createDefaultRichDesign();
 *
 *          PropertyDrawEntity propertyEntity = getPropertyDraw(someLP);
 *          PropertyDrawView propertyView = design.get(propertyView);
 *
 *          design.get(propertyView.toDraw).grid.highlighter = new Highlighter(Color.yellow, propertyView);
 *
 *          return design;
 *      }
 *  </code></pre>
 */
public class Highlighter implements ClientSerialize {
    private Color color;
    private PropertyDrawView property;

    public Highlighter(Color color, PropertyDrawView property) {
        this.color = color;
        this.property = property;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeUTF(property.getSID());
        new ObjectOutputStream(outStream).writeObject(color);
    }
}
