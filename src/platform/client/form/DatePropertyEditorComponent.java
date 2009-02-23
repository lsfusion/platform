package platform.client.form;

import com.toedter.calendar.JTextFieldDateEditor;

import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.event.KeyEvent;

class DatePropertyEditorComponent extends JTextFieldDateEditor {

    public DatePropertyEditorComponent(String datePattern, String maskPattern, char placeholder) {
        super(datePattern, maskPattern, placeholder);

        setBorder(new EmptyBorder(0, 1, 0, 0));

/*        SwingUtils.addFocusTraversalKey(this,
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));*/

    }

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {

        // не ловим ввод, чтобы его словил сам JTable и обработал
        return ke.getKeyCode() != KeyEvent.VK_ENTER && super.processKeyBinding(ks, ke, condition, pressed);
    }

    //а вот так будем дурить их protected метод
    public boolean publicProcessKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        return processKeyBinding(ks, ke, condition, pressed);
    }

/*    @Override
    public void focusLost(FocusEvent focusEvent) {
        super.focusLost(focusEvent);
    }*/

}
