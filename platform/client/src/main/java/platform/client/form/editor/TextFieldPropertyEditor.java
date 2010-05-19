package platform.client.form.editor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.KeyEvent;
import java.awt.*;

class TextFieldPropertyEditor extends JFormattedTextField {

    TextFieldPropertyEditor(Font font) {
        super();

        setBorder(new EmptyBorder(0, 3, 0, 0));
        setOpaque(true);

        if (font != null)
            setFont(font);
//        setBackground(new Color(128,128,255));
    }

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {

        // не ловим ввод, чтобы его словил сам JTable и обработал
        return ke.getKeyCode() != KeyEvent.VK_ENTER && super.processKeyBinding(ks, ke, condition, pressed);
    }

}
