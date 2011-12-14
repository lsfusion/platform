package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.EventObject;

public class BitPropertyEditor extends JCheckBox
                        implements PropertyEditorComponent {

    private boolean isNull = false;

    public BitPropertyEditor(Object value) {

        setHorizontalAlignment(JCheckBox.CENTER);
//        setVerticalAlignment(JCheckBox.CENTER);

        setOpaque(true);

        setBackground(Color.white);

        addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON3) {
                    setSelected(false);
                    isNull = true;
                    setBackground(Color.lightGray);
                }
            }
        });

        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                isNull = false;
                setBackground(Color.white);
            }
        });

        if (value != null)
            setSelected((Boolean)value);
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return this;
    }

    public Object getCellEditorValue() {

        if (isNull) return null;
        return isSelected();
    }

    public boolean valueChanged() {
        return true;
    }

    @Override
    public String checkValue(Object value){
        return null;
    }

}
