package lsfusion.client.base.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FlatRolloverButton extends JButton {
    private boolean showBackground;
    private MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            ((JButton) e.getSource()).setContentAreaFilled(true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            ((JButton) e.getSource()).setContentAreaFilled(false);
            if(showBackground) //setContentAreaFilled(false) do setOpaque(false)
                setOpaque(true);
        }
    };

    public FlatRolloverButton() {
        this(null, null);
    }

    public FlatRolloverButton(String text) {
        this(null, text);
    }

    public FlatRolloverButton(Icon icon) {
        this(icon, null);
    }

    public FlatRolloverButton(Action a) {
        this();
        setAction(a);
    }

    public FlatRolloverButton(Icon icon, String text) {
        super(text, icon);
        setContentAreaFilled(false);
        addMouseListener(mouseAdapter);
    }

    public void showBackground(boolean showBackground) {
        this.showBackground = showBackground;
        setOpaque(showBackground);
        setBackground(showBackground ? new Color(4, 137, 186, 24) : null);
        setBorder(showBackground ? BorderFactory.createLineBorder(new Color(204,204,204)) : null);
    }
}
