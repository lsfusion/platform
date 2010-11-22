package platform.interop;

import platform.base.context.ContextObject;
import platform.base.context.ApplicationContext;

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

    public ComponentDesign(){
    }

    public ComponentDesign(ApplicationContext context){
        this.context = context;
    }

    public void designCell(JComponent comp) {
        designComponent(comp, Color.white); // а то по умолчанию background у Label - серый
    }

    public void designComponent(JComponent comp) {
        designComponent(comp, null);
    }

    public void designComponent(JComponent comp, Color defaultBackground) {
        
        if (font != null) {
            comp.setFont(font);
        }

        if (background != null) {
            comp.setBackground(background);
            comp.setOpaque(true);
        }
        else if (defaultBackground != null)
            comp.setBackground(defaultBackground);

        if (foreground != null)
            comp.setForeground(foreground);
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

    public Font getHeaderFont(){
        return headerFont;
    }

    public void setHeaderFont(Font font){
        this.headerFont = font;
        updateDependency(this, "headerFont");
    }
}
