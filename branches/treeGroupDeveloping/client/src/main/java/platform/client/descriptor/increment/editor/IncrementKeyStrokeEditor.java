package platform.client.descriptor.increment.editor;

import platform.base.BaseUtils;
import platform.client.descriptor.editor.KeyInputDialog;
import platform.base.context.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class IncrementKeyStrokeEditor extends JPanel implements IncrementView {
    private final ApplicationContextProvider object;
    private final String field;
    private KeyStroke key;
    private String keyStrokeText, keyString;
    private JLabel title = new JLabel("Текущее сочетание: ");
    private JLabel keyAct = new JLabel();
    private JButton putKey = new JButton();

    public IncrementKeyStrokeEditor(ApplicationContextProvider object, String field){
        this.object = object;
        this.field = field;
        object.getContext().addDependency(object, field, this);
        fill();
    }

    public void fill() {
        setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        putKey.setText("Назначить");
        putKey.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae){
                KeyInputDialog keyInput = new KeyInputDialog(null);
                keyStrokeText = keyInput.showDialog();
                if(keyStrokeText != null){
                    key = KeyStroke.getKeyStroke(keyStrokeText);
                    transform();
                    updateField();
                }
            }
        });
        panel.add(title);
        panel.add(keyAct);
        panel.add(putKey);
        panel.add(new IncrementCheckBox("Показывать ключ", object, "showEditKey"));
        add(panel, BorderLayout.WEST);
    }

    private void transform(){
        if(key != null){
            keyString = "";
            if(keyStrokeText.contains("ctrl")) {
                keyString += "Ctrl + ";
            }
            if(keyStrokeText.contains("alt")) {
                keyString += "Alt + ";
            }
            if(keyStrokeText.contains("shift")) {
                keyString += "Shift + ";
            }
            String button = keyStrokeText.substring(keyStrokeText.lastIndexOf(' ') + 1);
            if(!button.equals("ALT") && !button.equals("CONTROL") && !button.equals("SHIFT")) {
                keyString += button;
            }
            else {
                keyString = keyString.substring(0, keyString.length() - 3);
            }
            keyAct.setText(keyString);
        }
    }

    private void updateField() {
        BaseUtils.invokeSetter(object, field, key);
    }

    public void update(Object updateObject, String updateField) {
        key = (KeyStroke) BaseUtils.invokeGetter(object, field);
        if(key != null){
            keyStrokeText = key.toString();
            transform();
        }
    }
}
