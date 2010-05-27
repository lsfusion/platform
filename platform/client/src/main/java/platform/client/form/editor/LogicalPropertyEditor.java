package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.EventObject;

public class LogicalPropertyEditor extends JCheckBox
                        implements PropertyEditorComponent {

    public LogicalPropertyEditor(Object value) {

        setHorizontalAlignment(JCheckBox.CENTER);
//        setVerticalAlignment(JCheckBox.CENTER);

        setOpaque(true);
        setBackground(Color.white);

        setSelected(value!=null);
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        if (editEvent == null) { // программно вызвали editCellAt
            setSelected(!isSelected());
            return null;
        } else
            return this;
    }

    public Object getCellEditorValue() {
        return isSelected()?true:null;
    }

    public boolean valueChanged() {
        return true;
    }
}
