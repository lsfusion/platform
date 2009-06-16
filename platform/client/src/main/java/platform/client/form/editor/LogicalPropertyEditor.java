package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LogicalPropertyEditor extends JCheckBox
                        implements PropertyEditorComponent {

    public LogicalPropertyEditor(Object value) {

        setHorizontalAlignment(JCheckBox.CENTER);
//        setVerticalAlignment(JCheckBox.CENTER);

        setOpaque(true);
        setBackground(Color.white);

        setSelected(value!=null);
    }

    public Component getComponent() {
        return this;
    }

    public Object getCellEditorValue() {
        return isSelected()?true:null;
    }

    public boolean valueChanged() {
        return true;
    }
}
