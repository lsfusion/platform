package platform.client.form;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.KeyEvent;

class TextFieldPropertyEditor extends JFormattedTextField {

    TextFieldPropertyEditor() {
        super();
        setBorder(new EmptyBorder(0, 3, 0, 0));
        setOpaque(true);
//        setBackground(new Color(128,128,255));
    }

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {

        // не ловим ввод, чтобы его словил сам JTable и обработал
        return ke.getKeyCode() != KeyEvent.VK_ENTER && super.processKeyBinding(ks, ke, condition, pressed);
    }

}
