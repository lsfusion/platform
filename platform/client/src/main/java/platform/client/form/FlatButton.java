package platform.client.form;

import javax.swing.*;
import java.awt.*;

public class FlatButton extends JButton {

    public FlatButton(String caption){
        super(caption);
        setFocusable(false);
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
    }
}
