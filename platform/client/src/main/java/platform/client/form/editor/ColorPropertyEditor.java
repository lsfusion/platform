package platform.client.form.editor;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditorComponent;
import platform.client.logics.classes.ClientColorClass;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.EventObject;

public class ColorPropertyEditor extends JTextField implements PropertyEditorComponent {
    private Color initialColor;
    private Color chosenColor;
    private JColorChooser editorDialog;
    private JDialog chooserDialog;
    private boolean reset;

    public ColorPropertyEditor(Object value) {
        super();
        initialColor = (Color) value;

        setBackground(initialColor != null ? initialColor : ClientColorClass.getDefaultValue());
        editorDialog = new JColorChooser(initialColor != null ? initialColor : ClientColorClass.getDefaultValue());

        ActionListener okListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setBackground(editorDialog.getColor());
                chosenColor = editorDialog.getColor();
                reset = false;
            }
        };

        ActionListener cancelListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        };

        chooserDialog = JColorChooser.createDialog(null, ClientResourceBundle.getString("form.choose.color"), true, editorDialog, okListener, cancelListener);
    }

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        if (condition == WHEN_FOCUSED && KeyEvent.VK_ESCAPE != ke.getKeyChar() && KeyEvent.VK_ENTER != ke.getKeyChar() && KeyEvent.CHAR_UNDEFINED != ke.getKeyChar() ) {
            if (ke.getKeyChar() == KeyEvent.VK_DELETE) {
                setBackground(ClientColorClass.getDefaultValue());
                chosenColor = null;
                editorDialog.setColor(ClientColorClass.getDefaultValue());
                reset = true;
            } else {
                chooserDialog.setVisible(true);
            }
            return true;
        } else {
            return super.processKeyBinding(ks, ke, condition, pressed);
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            chooserDialog.setVisible(true);
        } else {
            super.processMouseEvent(e);
        }
    }

    @Override
    protected void processFocusEvent(FocusEvent e) {
        super.processFocusEvent(e);
        getCaret().setVisible(false);
    }

    @Override
    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return this;
    }

    @Override
    public Object getCellEditorValue() throws RemoteException {
        if (chosenColor == null) {
            return reset ? chosenColor : initialColor;
        } else {
            return chosenColor;
        }
    }

    @Override
    public boolean valueChanged() {
        return true;
    }

    @Override
    public String checkValue(Object value) {
        return null;
    }
}
