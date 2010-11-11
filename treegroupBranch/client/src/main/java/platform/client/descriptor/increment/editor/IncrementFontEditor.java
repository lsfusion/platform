package platform.client.descriptor.increment.editor;

import platform.base.BaseUtils;
import platform.client.descriptor.editor.FontChooser;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.increment.IncrementView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.util.Map;

public class IncrementFontEditor extends JPanel implements IncrementView {
    private final Object object;
    private final String field;
    private Font font;
    private JLabel title = new JLabel();
    private JTextField textField = new JTextField();
    private JButton chooseFont = new JButton();

    public IncrementFontEditor(String title, Object object, String field){
        this.title.setText(title);
        this.object = object;
        this.field = field;
        IncrementDependency.add(object, field, this);
        fill();
    }

    private void setText(){
        textField.setText("AaBbCc АаБбВв 123");
        Map attributes;
        attributes = font.getAttributes();
        attributes.put(TextAttribute.SIZE, 16);
        Font tempFont = new Font(font.getFontName(), font.getStyle(), 16).deriveFont(attributes);
        textField.setFont(tempFont);
    }

    private void fill(){
        if(font != null){
            setText();
        }
        textField.setEditable(false);
        chooseFont.setText("Выбрать..");
        chooseFont.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                FontChooser chooser = new FontChooser(null, font);
                if(chooser.showDialog()){
                    font = chooser.getFont();
                }
                if(font != null){
                    setText();
                    updateField();
                }
            }
        });
        setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(title, BorderLayout.WEST);
        panel.add(textField, BorderLayout.CENTER);
        panel.add(chooseFont, BorderLayout.EAST);
        add(panel, BorderLayout.CENTER);
    }

    private void updateField() {
        BaseUtils.invokeSetter(object, field, font);
    }

    public void update(Object updateObject, String updateField) {
        Font newFont = (Font) BaseUtils.invokeGetter(object, field);
            font = newFont;
    }
}
