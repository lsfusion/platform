package lsfusion.interop.form.design;

import lsfusion.base.ResourceUtils;
import lsfusion.base.context.ContextObject;
import lsfusion.base.file.SerializableImageIconHolder;
import lsfusion.interop.base.view.ColorTheme;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

public class ComponentDesign extends ContextObject implements Serializable {

    public FontInfo font;

    public FontInfo captionFont;

    public Color background;
    public Color foreground;

    private SerializableImageIconHolder imageHolder;

    public ComponentDesign() {
    }

    public ImageIcon getImage() {
        return imageHolder != null ? imageHolder.getImage() : null;
    }

    public ImageIcon getImage(ColorTheme colorTheme) {
        return imageHolder != null ? imageHolder.getImage(colorTheme) : null;
    }

    public void setImage(String imagePath) {
        ImageIcon image = ResourceUtils.readImage(imagePath);
        if (image != null) {
            if (imageHolder == null) {
                imageHolder = new SerializableImageIconHolder(image, imagePath);
            } else {
                imageHolder.setImage(image, imagePath);
            }
        }
    }

    public SerializableImageIconHolder getImageHolder() {
        return imageHolder;
    }

    public void designCell(JComponent comp) {
        designComponent(comp);
    }

    public void designButton(JButton comp, ColorTheme colorTheme) {
        designComponent(comp);
        
        ImageIcon image = getImage(colorTheme);
        if (image != null) {
            comp.setIcon(image);
        }
    }

    public void designComponent(JComponent comp) {
        if (font != null) {
            comp.setFont(getFont(comp));
        }

        if (background != null) {
            comp.setBackground(background);
            comp.setOpaque(true);
        } else {
            comp.setBackground(null); // default transparent background
        }

        if (foreground != null) {
            comp.setForeground(foreground);
        }
    }

    public void designHeader(Component comp) {
        if (captionFont != null) {
            comp.setFont(getCaptionFont(comp));
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

    public Font getCaptionFont(Component component) {
        return getOrDeriveComponentFont(captionFont, component);
    }

    public FontInfo getFont() {
        return font;
    }

    public void setFont(FontInfo font) {
        this.font = font;
        updateDependency(this, "font");
    }

    public FontInfo getCaptionFont() {
        return captionFont;
    }

    public void setCaptionFont(FontInfo font) {
        this.captionFont = font;
        updateDependency(this, "captionFont");
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
}
