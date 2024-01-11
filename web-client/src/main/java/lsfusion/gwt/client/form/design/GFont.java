package lsfusion.gwt.client.form.design;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.FontContext;

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

    public static GFont getFont(GPropertyDraw property, FontContext fontContext) {
        return property.font != null ? property.font : fontContext.getFont();
    }

    public static void setFont(Element element, GFont font) {
        if(font != null) {
            element.addClassName("cell-with-custom-font");

            setCellFont(element, font.family, font.size, font.italic, font.bold);
        } else {
            clearCellFont(element);
        }
    }

    public static void clearFont(Element element) {
        element.removeClassName("cell-with-custom-font");
        clearCellFont(element);
    }

    private static native void setCellFont(Element element, String family, int size, boolean italic, boolean bold)/*-{
        if (family != null)
            element.style.setProperty("--custom-font-family", family);
        if (size > 0)
            element.style.setProperty("--custom-font-size", size + "px");
        element.style.setProperty("--custom-font-style", italic ? "italic" : "normal");
        element.style.setProperty("--custom-font-weight", bold ? "bold" : "normal");
    }-*/;

    private static native void clearCellFont(Element element)/*-{
        element.style.removeProperty("--custom-font-family");
        element.style.removeProperty("--custom-font-size");
        element.style.removeProperty("--custom-font-style");
        element.style.removeProperty("--custom-font-weight");
    }-*/;
    
    public GFont deriveFont(boolean bold, boolean italic) {
        return new GFont(family, size, bold, italic);
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
