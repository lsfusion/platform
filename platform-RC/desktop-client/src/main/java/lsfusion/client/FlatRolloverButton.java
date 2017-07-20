package lsfusion.client;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FlatRolloverButton extends JButton {
    private MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            ((JButton) e.getSource()).setContentAreaFilled(true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            ((JButton) e.getSource()).setContentAreaFilled(false);
        }
    };

    public FlatRolloverButton() {
        this(null, null);
    }

    public FlatRolloverButton(String text) {
        this(text, null);
    }

    public FlatRolloverButton(Icon icon) {
        this(null, icon);
    }

    public FlatRolloverButton(Action a) {
        this();
        setAction(a);
    }

    public FlatRolloverButton(String text, Icon icon) {
        super(text, icon);
        setContentAreaFilled(false);
        addMouseListener(mouseAdapter);
    }
}
