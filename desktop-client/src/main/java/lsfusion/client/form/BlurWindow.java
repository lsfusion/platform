package lsfusion.client.form;

import java.awt.*;
import javax.swing.*;

public class BlurWindow extends JDialog {

    public BlurWindow(Window parent) {
        super(parent, ModalityType.MODELESS);
        setLayout(new GridBagLayout());
        setUndecorated(true);
        setOpacity(0.5f);
        setSize(parent.getSize());
        setBackground(new Color(0, 0, 0, 128));
        setFocusableWindowState(false);
    }
}
