package platform.client.descriptor.editor.base;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public abstract class FlatButton extends JButton implements ActionListener {

    protected FlatButton() {
    }

    public FlatButton(String caption){
        super(caption);
        setFocusable(false);
        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        addActionListener(this);
    }
}
