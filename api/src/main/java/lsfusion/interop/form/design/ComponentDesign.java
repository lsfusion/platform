package lsfusion.interop.form.design;

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

    public void setImage(String imagePath) {
        if (imageHolder == null)
            imageHolder = new SerializableImageIconHolder();
        imageHolder.setImage(imagePath);
    }

    public SerializableImageIconHolder getImageHolder() {
        return imageHolder;
    }

    public void installFont(JComponent comp) {
        if (font != null) {
            comp.setFont(getFont(comp));
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
