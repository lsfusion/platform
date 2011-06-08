package platform.client.descriptor.editor;

import platform.base.context.ApplicationContextProvider;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementTextEditor;

import javax.swing.*;
import java.awt.*;

public class SizesEditor extends TitledPanel {

    public SizesEditor(ApplicationContextProvider object) {
        super("Размеры");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new DimensionEditor("минимальные", object, "minimum"));
        add(new DimensionEditor("максимальные", object, "maximum"));
        add(new DimensionEditor("предпочтительные", object, "preferred"));
    }

    public class DimensionEditor extends TitledPanel {
        public DimensionEditor(String title, ApplicationContextProvider object, String fieldPrefix) {
            super(title);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(new JLabel("Ширина: "));
            add(new IncrementTextEditor(object, fieldPrefix + "Width"));
            add(Box.createRigidArea(new Dimension(5, 5)));
            add(new JLabel("Высота: "));
            add(new IncrementTextEditor(object, fieldPrefix + "Height"));
        }
    }
}
