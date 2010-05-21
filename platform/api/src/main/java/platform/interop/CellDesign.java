package platform.interop;

import javax.swing.*;
import java.io.Serializable;
import java.text.Format;
import java.awt.*;

public class CellDesign implements Serializable {

    public Font font;
    public Font getFont(JComponent comp) {
        return (font == null ? comp.getFont() : font);
    }

    public Color background;
    public Color foreground;

    public void designComponent(JComponent comp) {
        
        if (font != null) {
            comp.setFont(font);
        }

        if (background != null)
            comp.setBackground(background);
        else
            comp.setBackground(Color.white); // а то по умолчанию background у Label - серый

        if (foreground != null)
            comp.setForeground(foreground);
    }
}
