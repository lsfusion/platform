package lsfusion.client.form.editor;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;

public class EditorEventQueue extends EventQueue {
    protected void dispatchEvent(AWTEvent event) {
        super.dispatchEvent(event);

        if (!(event instanceof MouseEvent)) {
            return;
        }

        MouseEvent me = (MouseEvent) event;

        if (!me.isPopupTrigger()) {
            return;
        }

        Component comp = SwingUtilities.getDeepestComponentAt(me.getComponent(), me.getX(), me.getY());

        if (!(comp instanceof JTextComponent)) {
            return;
        }

        if (MenuSelectionManager.defaultManager().getSelectedPath().length > 0) {
            return;
        }

        JTextComponent tc = (JTextComponent) comp;
        JPopupMenu menu = new EditorContextMenu(tc);

        Point pt = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), tc);
        menu.show(tc, pt.x, pt.y);
    }
} 
