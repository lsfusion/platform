package platform.client.descriptor.increment.editor;

import platform.base.BaseUtils;
import platform.client.ClientResourceBundle;
import platform.client.descriptor.editor.KeyInputDialog;
import platform.base.context.*;
import platform.client.descriptor.editor.base.FlatButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class IncrementKeyStrokeEditor extends JPanel implements IncrementView {

    private final ApplicationContextProvider object;
    private final String field;
    private KeyStroke key;
    private String keyStrokeText;
    private JLabel title = new JLabel(ClientResourceBundle.getString("descriptor.editor.display.current.shortcut")+": ");
    private JLabel dropLabel = new JLabel(ClientResourceBundle.getString("descriptor.editor.display.reset"));

    private KeyStrokeButton button = new KeyStrokeButton();

    public IncrementKeyStrokeEditor(ApplicationContextProvider object, String field) {
        this.object = object;
        this.field = field;
        object.getContext().addDependency(object, field, this);

        dropLabel.addMouseListener(adapter);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(title);
        add(button);
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(dropLabel);
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.display.shortcut.keys"), object, "showEditKey"));
    }

    private MouseAdapter adapter = new MouseAdapter(){
        public void mouseEntered(MouseEvent e){
            e.getComponent().setForeground(Color.RED);
        }

        public void mouseExited(MouseEvent e){
            e.getComponent().setForeground(Color.BLACK);
        }

        public void mouseClicked(MouseEvent e){
            key = null;
            button.transform();
            updateField();
        }
    };

    private void updateField() {
        BaseUtils.invokeSetter(object, field, key);
    }

    public void update(Object updateObject, String updateField) {
        key = (KeyStroke) BaseUtils.invokeGetter(object, field);
        if (key != null) {
            keyStrokeText = key.toString();
            button.transform();
        }
    }

    private class KeyStrokeButton extends FlatButton {

        private String keyString;

        public void onClick() {
            KeyInputDialog keyInput = new KeyInputDialog(null);
            keyStrokeText = keyInput.showDialog();
            if (keyStrokeText != null) {
                key = KeyStroke.getKeyStroke(keyStrokeText);
                transform();
                updateField();
            }
        }

        private void transform() {
            if (key != null) {
                keyString = "";
                if (keyStrokeText.contains("ctrl")) {
                    keyString += "Ctrl + ";
                }
                if (keyStrokeText.contains("alt")) {
                    keyString += "Alt + ";
                }
                if (keyStrokeText.contains("shift")) {
                    keyString += "Shift + ";
                }
                String button = keyStrokeText.substring(keyStrokeText.lastIndexOf(' ') + 1);
                if (!button.equals("ALT") && !button.equals("CONTROL") && !button.equals("SHIFT")) {
                    keyString += button;
                } else {
                    keyString = keyString.substring(0, keyString.length() - 3);
                }
                setText(keyString);
            }
            else{
                setText("");
            }
        }
    }
}
