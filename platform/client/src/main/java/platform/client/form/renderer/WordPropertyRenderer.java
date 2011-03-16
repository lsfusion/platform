package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.text.Format;

public class WordPropertyRenderer extends FilePropertyRenderer
        implements PropertyRendererComponent {

    private ImageIcon wordIcon;

    public WordPropertyRenderer(Format format, ComponentDesign design) {
        super(format, design);
        wordIcon = new ImageIcon(WordPropertyRenderer.class.getResource("/images/word.jpeg"));
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setIcon(wordIcon);
        } else {
            setIcon(null);
        }
        setSelected(isSelected, hasFocus);
    }
}
