package platform.interop;

import platform.base.context.ApplicationContext;
import platform.base.context.ContextObject;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

public class ComponentDesign extends ContextObject implements Serializable {

    public Font font;

    public Font getFont(JComponent comp) {
        return (font == null ? comp.getFont() : font);
    }

    public Font headerFont;

    public Color background;
    public Color foreground;
    private SerializableImageIconHolder imageHolder;
    public String iconPath;

    public ComponentDesign() {
    }

    public ImageIcon getImage() {
        return imageHolder != null ? imageHolder.getImage() : null;
    }

    public void setImage(ImageIcon image) {
        if (imageHolder == null) {
            imageHolder = new SerializableImageIconHolder(image);
        } else {
            imageHolder.setImage(image);
        }
    }

    public SerializableImageIconHolder getImageHolder() {
        return imageHolder;
    }

    public ComponentDesign(ApplicationContext context) {
        this.context = context;
    }

    public void designCell(JComponent comp) {
        designComponent(comp, Color.white); // а то по умолчанию background у Label - серый
    }

    public void designComponent(JComponent comp) {
        designComponent(comp, null);
        if (getImage() != null && comp instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) comp;
            button.setIcon(getImage());
        }
    }

    public void designComponent(JComponent comp, Color defaultBackground) {

        if (font != null) {
            comp.setFont(font);
        }

        if (background != null) {
            comp.setBackground(background);
            comp.setOpaque(true);
        } else if (defaultBackground != null)
            comp.setBackground(defaultBackground);

        if (foreground != null)
            comp.setForeground(foreground);
    }

    public boolean isDefaultDesign() {
        return font == null && headerFont == null && background == null && foreground == null;
    }

    public void designHeader(Component comp) {
        if (headerFont != null)
            comp.setFont(headerFont);
    }

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color background) {
        this.background = background;
        updateDependency(this, "background");
    }

    public Color getForeground() {
        return foreground;
    }

    public void setForeground(Color foreground) {
        this.foreground = foreground;
        updateDependency(this, "foreground");
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
        updateDependency(this, "font");
    }

    public Font getHeaderFont() {
        return headerFont;
    }

    public void setHeaderFont(Font font) {
        this.headerFont = font;
        updateDependency(this, "headerFont");
    }

    public String getCodeBackground(String name) {
        return "design.setBackground(" + name + ", new Color(" + background.getRed() + ", " + background.getGreen() + ", " + background.getBlue() + "));\n";
    }

    public String getCodeForeground(String name) {
        return "design.setForeground(" + name + ", new Color(" + foreground.getRed() + ", " + foreground.getGreen() + ", " + foreground.getBlue() + "));\n";
    }

    public String getCodeFont(String name) {
        return "design.setFont(" + name + ", new Font(\"" + font.getName() + "\", " + getFontStyle(font) + ", " + font.getSize() + "));\n";
    }

    public String getCodeHeaderFont(String name) {
        return "design.setHeaderFont(" + name + ", new Font(\"" + headerFont.getName() + "\", " + getFontStyle(headerFont) + ", " + headerFont.getSize() + "));\n";
    }

    public String getFontStyle(Font font) {
        String strStyle = "";
        int style = font.getStyle();
        if (style == 0) {
            strStyle = "Font.PLAIN";
        } else {
            if ((style & Font.BOLD) != 0) {
                strStyle += "Font.BOLD";
            }
            if ((style & Font.ITALIC) != 0) strStyle += (((style & Font.BOLD) != 0) ? " | " : "") + "Font.ITALIC";
        }
        return strStyle;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
        setImage(new ImageIcon(ComponentDesign.class.getResource("/images/" + iconPath)));
    }
}
