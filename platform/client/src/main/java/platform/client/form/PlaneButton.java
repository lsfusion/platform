package platform.client.form;

import javax.swing.*;
import java.awt.*;

public class PlaneButton extends JButton {

    public PlaneButton(String caption){
        super(caption);
        setFocusable(false);
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
    }
}
