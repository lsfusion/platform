package lsfusion.client.descriptor.editor;

import lsfusion.base.context.ApplicationContextProvider;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementTextEditor;

import javax.swing.*;
import java.awt.*;

public class SizesEditor extends TitledPanel {

    public SizesEditor(ApplicationContextProvider object) {
        super(ClientResourceBundle.getString("descriptor.editor.sizeseditor.sizes"));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new DimensionEditor(ClientResourceBundle.getString("descriptor.editor.sizeseditor.minimum"), object, "minimum"));
        add(new DimensionEditor(ClientResourceBundle.getString("descriptor.editor.sizeseditor.maximum"), object, "maximum"));
        add(new DimensionEditor(ClientResourceBundle.getString("descriptor.editor.sizeseditor.preferred"), object, "preferred"));
    }

    public class DimensionEditor extends TitledPanel {
        public DimensionEditor(String title, ApplicationContextProvider object, String fieldPrefix) {
            super(title);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(new JLabel(ClientResourceBundle.getString("descriptor.editor.sizeseditor.width")+" "));
            add(new IncrementTextEditor(object, fieldPrefix + "Width"));
            add(Box.createRigidArea(new Dimension(5, 5)));
            add(new JLabel(ClientResourceBundle.getString("descriptor.editor.sizeseditor.height")+" "));
            add(new IncrementTextEditor(object, fieldPrefix + "Height"));
        }
    }
}
