package platform.client.descriptor.editor.base;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class FlatButton extends JTextField {

    private void init() {
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

    protected FlatButton() {
        init();
    }

    public FlatButton(String caption){
        super(caption);

        init();
    }

    protected abstract void onClick();
}
