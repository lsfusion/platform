package lsfusion.client.form.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.SwingUtils;
import lsfusion.client.form.ClientPropertyTableEditorComponent;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.cell.PropertyTableCellEditor;
import lsfusion.interop.ComponentDesign;
import lsfusion.interop.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;


@SuppressWarnings({"FieldCanBeLocal"})
public class TextPropertyEditor extends JScrollPane implements PropertyEditor, PropertyChangeListener, ClientPropertyTableEditorComponent {
    private final int WIDTH = 250;
    private final int HEIGHT = 200;
    private String typedText;
    private JTextArea textArea;
    private JDialog dialog;

    private JOptionPane optionPane;

    private String btnSave = ClientResourceBundle.getString("form.editor.save");
    private String btnCancel = ClientResourceBundle.getString("form.editor.cancel");
    private boolean state;

    public TextPropertyEditor(Object value, ComponentDesign design) {
        this(null, value, design);
    }

    public TextPropertyEditor(Component owner, Object value, ComponentDesign design) {
        textArea = new JTextArea((String) value);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        setViewportView(textArea);
        setPreferredSize(new Dimension(200, 200));
        dialog = new JDialog(SwingUtils.getWindow(owner), Dialog.ModalityType.DOCUMENT_MODAL);
        textArea.setFont(new Font("Tahoma", Font.PLAIN, 12));
        if (design != null) {
            design.designCell(textArea);
        }


        String msgString1 = ClientResourceBundle.getString("form.editor.text");
        Object[] array = {msgString1, this};

        Object[] options = {btnSave, btnCancel};

        optionPane = new JOptionPane(array,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                null,
                options,
                options[0]);

        dialog.setContentPane(optionPane);
        dialog.setUndecorated(true);
        dialog.getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray, 1));
        optionPane.addPropertyChangeListener(this);
        setFocusable(true);
        textArea.getCaret().setVisible(true);

        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                textArea.requestFocusInWindow();
            }
        });

    }

    public void clearAndHide() {
        dialog.setVisible(false);
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        //пока не нужен
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        if (KeyStrokes.isSpaceEvent(editEvent)) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (int) Math.min(tableLocation.getX(), screenSize.getWidth() - WIDTH);
            dialog.setBounds(x, (int) tableLocation.getY(), WIDTH, HEIGHT);
            dialog.setVisible(true);
            return null;
        } else {
            return this;
        }
    }

    public Object getCellEditorValue() {
        String text = textArea.getText();
        return text.isEmpty() ? null : text;
    }

    public boolean valueChanged() {
        return state;
    }

   @Override
    public boolean stopCellEditing() {
        return true;
    }

    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();

        if (isVisible() && (e.getSource() == optionPane) && (JOptionPane.VALUE_PROPERTY.equals(prop) ||
                JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
            Object value = optionPane.getValue();

            if (value == JOptionPane.UNINITIALIZED_VALUE) {
                //ignore reset
                return;
            }

            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

            if (btnSave.equals(value)) {
                state = !(textArea.getText().equals(typedText));
                typedText = textArea.getText();
                clearAndHide();
            } else {
                state = false;
                typedText = null;
                clearAndHide();
            }
        }
    }

    @Override
    public boolean requestFocusInWindow() {
        return textArea.requestFocusInWindow();
    }

    public void prepareTextEditor(boolean clear) {
        if (clear) {
            textArea.setText("");
        } else {
            textArea.selectAll();
        }
    }
}
