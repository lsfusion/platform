package platform.client.form.editor;

import platform.interop.ComponentDesign;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.KeyEvent;

class TextFieldPropertyEditor extends JFormattedTextField {

    TextFieldPropertyEditor(ComponentDesign design) {
        super();

        setBorder(new EmptyBorder(0, 3, 0, 0));
        setOpaque(true);

        if (design != null)
            design.designCell(this);
    }

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {

        // не ловим ввод, чтобы его словил сам JTable и обработал
        return (ke.getKeyCode() != KeyEvent.VK_ENTER && ke.getKeyCode() != KeyEvent.VK_ESCAPE) && super.processKeyBinding(ks, ke, condition, pressed);
    }

}
