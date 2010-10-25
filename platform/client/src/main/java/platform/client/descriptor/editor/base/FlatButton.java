package platform.client.descriptor.editor.base;

import platform.base.BaseUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class FlatButton extends JTextField {

    protected FlatButton() {
    }

    public FlatButton(String caption){
        super(caption);
        setFocusable(false);
        setEditable(false);
//        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClick();
            }
        });
/*        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClick();
            }
        });*/
    }

    protected abstract void onClick();
}
