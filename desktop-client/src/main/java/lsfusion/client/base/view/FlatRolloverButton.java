package lsfusion.client.base.view;

import lsfusion.client.form.design.view.widget.ButtonWidget;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FlatRolloverButton extends ButtonWidget {
    private boolean showBackground;

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
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!showBackground) {
                    setContentAreaFilled(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!showBackground) {
                    setContentAreaFilled(false);
                }
            }
        });
    }

    public void showBackground(boolean showBackground) {
        if (this.showBackground != showBackground) {
            this.showBackground = showBackground;
            setContentAreaFilled(showBackground);
            updateBackground();
        }
    }
    
    public void updateBackground() {
        setBackground(showBackground ? SwingDefaults.getSelectionColor() : null);
        setBorder(showBackground ? SwingDefaults.getButtonBorder() : null);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (showBackground) {
            updateBackground();
        }
    }
}
