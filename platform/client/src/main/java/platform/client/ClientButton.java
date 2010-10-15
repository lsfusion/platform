package platform.client;

import javax.swing.*;

public class ClientButton extends JButton {

    public ClientButton(Action a) {
        super(a);
        overrideModel();
    }

    public ClientButton(String name) {
        super(name);
        overrideModel();
    }

    public ClientButton() {
        overrideModel();
    }

    public ClientButton(ImageIcon deleteIcon) {
        overrideModel();
    }

    private void overrideModel() {

        setModel(new DefaultButtonModel() {

            @Override
            public void setPressed(boolean b) {
                SwingUtils.commitCurrentEditing();
                super.setPressed(b);
            }
        });
    }
}
