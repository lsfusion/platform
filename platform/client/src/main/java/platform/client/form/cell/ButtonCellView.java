package platform.client.form.cell;

import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class ButtonCellView extends JButton implements CellView {
    public ButtonCellView(final ClientPropertyDraw key, final ClientFormController form) {
        super(key.getFullCaption());

        if (key.readOnly) {
            setEnabled(false);
        }

        key.design.designComponent(this);

        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {

                try {

                    PropertyEditorComponent editor = key.getEditorComponent(form, null);
                    if (editor != null) {
                        editor.getComponent(SwingUtils.computeAbsoluteLocation(ButtonCellView.this), getBounds(), null);
                        if (editor.valueChanged())
                            listener.cellValueChanged(editor.getCellEditorValue());
                    }

                } catch (Exception e) {
                    throw new RuntimeException("Ошибка при выполнении действия", e);
                }
            }
        });
    }

    public JComponent getComponent() {
        return this;
    }

    private CellViewListener listener;
    public void addListener(CellViewListener listener) {
        this.listener = listener;
    }

    public void setValue(Object ivalue) {
        // собственно, а как в Button нужно устанавливать value
    }

    public void startEditing(KeyEvent e) {
        doClick(20);
    }
}
