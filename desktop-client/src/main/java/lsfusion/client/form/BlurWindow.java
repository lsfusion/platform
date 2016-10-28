package lsfusion.client.form;

import javax.swing.*;
import java.awt.*;

public class BlurWindow extends JDialog {

    public BlurWindow(Window parent) {
        super(parent, ModalityType.MODELESS);
        setLayout(new GridBagLayout());
        setUndecorated(true);
        setSize(parent.getSize());
        setBackground(new Color(0, 0, 0, 64));
        setFocusableWindowState(false);
    }
}
