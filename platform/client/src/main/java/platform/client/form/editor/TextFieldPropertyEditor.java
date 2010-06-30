package platform.client.form.editor;

import platform.interop.CellDesign;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.KeyEvent;

class TextFieldPropertyEditor extends JFormattedTextField {

    TextFieldPropertyEditor(CellDesign design) {
        super();

        setBorder(new EmptyBorder(0, 3, 0, 0));
        setOpaque(true);

        if (design != null)
            design.designComponent(this);
    }

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {

        // не ловим ввод, чтобы его словил сам JTable и обработал
        return ke.getKeyCode() != KeyEvent.VK_ENTER && super.processKeyBinding(ks, ke, condition, pressed);
    }

}
