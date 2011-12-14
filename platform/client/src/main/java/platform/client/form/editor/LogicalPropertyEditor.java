package platform.client.form.editor;

import platform.client.SwingUtils;
import platform.client.form.PropertyEditorComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.EventObject;

public class LogicalPropertyEditor extends JCheckBox implements PropertyEditorComponent {

    public LogicalPropertyEditor(Object value) {

        setHorizontalAlignment(JCheckBox.CENTER);
//        setVerticalAlignment(JCheckBox.CENTER);

        setOpaque(true);
        setBackground(Color.white);

        setSelected(value != null);

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtils.commitCurrentEditing();
            }
        });
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        if (editEvent == null) { // программно вызвали editCellAt
            setSelected(!isSelected());
            return null;
        } else {
            return this;
        }
    }

    public Object getCellEditorValue() {
        return isSelected() ? true : null;
    }

    public boolean valueChanged() {
        return true;
    }

   @Override
    public String checkValue(Object value){
        return null;
    }
}
