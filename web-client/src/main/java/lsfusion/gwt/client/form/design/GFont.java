package lsfusion.gwt.client.form.design;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;

import java.io.Serializable;

import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public class GFont implements Serializable {

    public String family;
    public int size;
    public boolean bold;
    public boolean italic;

    public GFont() {
    }

    public GFont(String family, Integer size, boolean bold, boolean italic) {
        this.family = family;
        this.size = size;
        this.bold = bold;
        this.italic = italic;
    }

    private native void applyCustom(Element element, String family, int size, boolean italic, boolean bold)/*-{
        element.classList.add("custom-renderer-font");
        if (family != null)
            element.style.setProperty("--custom-element-font-family", family);
        if (size > 0)
            element.style.setProperty("--custom-element-font-size", size + "px");
        element.style.setProperty("--custom-element-font-style", italic ? "italic" : "normal");
        element.style.setProperty("--custom-element-font-weight", bold ? "bold" : "normal");
    }-*/;

    private native void clearCustom(Element element, String family, int size, boolean italic, boolean bold)/*-{
        element.classList.remove("custom-renderer-font");
        if (family != null)
            element.style.removeProperty("--custom-element-font-family");
        if (size > 0)
            element.style.removeProperty("--custom-element-font-size");
        element.style.removeProperty("--custom-element-font-style");
        element.style.removeProperty("--custom-element-font-weight");
    }-*/;

    public void applyCustom(Element element) {
        applyCustom(element, family, size, italic, bold);
    }

    public void clearCustom(Element element) {
        clearCustom(element, family, size, italic, bold);
    }

    public void apply(Style style) {
        if (family != null) {
            style.setProperty("fontFamily", family);
        }
        if (size > 0) {
            style.setFontSize(size, Style.Unit.PX);
        }
        style.setFontStyle(italic ? Style.FontStyle.ITALIC : Style.FontStyle.NORMAL);
        style.setFontWeight(bold ? Style.FontWeight.BOLD : Style.FontWeight.NORMAL);
    }
    public void clear(Style style) {
        if (family != null) {
            style.clearProperty("fontFamily");
        }
        if (size > 0) {
            style.clearFontSize();
        }
        style.clearFontSize();
        style.clearFontWeight();
    }
    
    public GFont deriveFont(boolean bold, boolean italic) {
        return new GFont(family, size, bold, italic);
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    transient private int hash;
    transient private boolean hashComputed;

    @Override
    public int hashCode() {
        if (hashComputed) {
            hash = family != null ? family.hashCode() : 0;
            hash = 31 * hash + (size);
            hash = 31 * hash + (bold ? 1 : 0);
            hash = 31 * hash + (italic ? 1 : 0);
            hashComputed = true;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof GFont)) {
            return false;
        }
        GFont font = (GFont) obj;
        return bold == font.bold &&
               italic == font.italic &&
               nullEquals(family, font.family) &&
               size == font.size;
    }
}
