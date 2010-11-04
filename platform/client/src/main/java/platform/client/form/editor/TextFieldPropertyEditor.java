package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;
import platform.interop.ComponentDesign;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;

abstract class TextFieldPropertyEditor extends JFormattedTextField implements PropertyEditorComponent {
    private boolean selected = false;

    TextFieldPropertyEditor(ComponentDesign design) {
        super();

        setBorder(new EmptyBorder(0, 3, 0, 0));
        setOpaque(true);

        if (design != null) {
            design.designCell(this);
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                //чтобы выделялся текст при активации компонента мышкой
                if (!selected) {
                    selectAll();
                    selected = true;
                }
            }
        });
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        selected = false;
        //для очистки поля ввода перед записью новых данных
//        if (editEvent instanceof KeyEvent) {
//            KeyEvent event = (KeyEvent) editEvent;
//            if (event.getKeyChar() != KeyEvent.CHAR_UNDEFINED &&
//                event.getKeyChar() != KeyEvent.VK_DELETE &&
//                event.getKeyChar() != KeyEvent.VK_BACK_SPACE){
//                setValue(null);
//            }
//        }
        return this;
    }

    public boolean valueChanged() {
        return true;
    }

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        // не ловим ввод, чтобы его словил сам JTable и обработал
        return (ke.getKeyCode() != KeyEvent.VK_ENTER && ke.getKeyCode() != KeyEvent.VK_ESCAPE) && super.processKeyBinding(ks, ke, condition, pressed);
    }
}
