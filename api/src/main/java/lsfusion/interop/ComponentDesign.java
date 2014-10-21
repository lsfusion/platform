package lsfusion.interop;

import lsfusion.base.context.ApplicationContext;
import lsfusion.base.context.ContextObject;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

public class ComponentDesign extends ContextObject implements Serializable {

    public FontInfo font;

    public FontInfo headerFont;

    public Color background;
    public Color foreground;

    public String iconPath;
    private SerializableImageIconHolder imageHolder;

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

    private void designComponent(JComponent comp, Color defaultBackground) {

        if (font != null) {
            comp.setFont(getFont(comp));
        }

        if (background != null) {
            comp.setBackground(background);
            comp.setOpaque(true);
        } else if (defaultBackground != null) {
            comp.setBackground(defaultBackground);
        }

        if (foreground != null) {
            comp.setForeground(foreground);
        }
    }

    public void designHeader(Component comp) {
        if (headerFont != null) {
            comp.setFont(getHeaderFont(comp));
        }
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

    public Font getFont(Component component) {
        return getOrDeriveComponentFont(font, component);
    }

    public Font getHeaderFont(Component component) {
        return getOrDeriveComponentFont(headerFont, component);
    }

    public FontInfo getFont() {
        return font;
    }

    public void setFont(FontInfo font) {
        this.font = font;
        updateDependency(this, "font");
    }

    public FontInfo getHeaderFont() {
        return headerFont;
    }

    public void setHeaderFont(FontInfo font) {
        this.headerFont = font;
        updateDependency(this, "headerFont");
    }

    private Font getOrDeriveComponentFont(FontInfo fontInfo, Component component) {
        if (fontInfo == null) {
            return component.getFont();
        }

        Object oFont = component instanceof JComponent ? ((JComponent) component).getClientProperty(fontInfo) : null;
        if (oFont instanceof Font) {
            return (Font) oFont;
        }

        Font cFont = fontInfo.deriveFrom(component);
        if (component instanceof JComponent) {
            ((JComponent) component).putClientProperty(fontInfo, cFont);
        }
        return cFont;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
        setImage(new ImageIcon(ComponentDesign.class.getResource("/images/" + iconPath)));
    }
}
