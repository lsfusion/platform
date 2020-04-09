package lsfusion.gwt.client.form.design;

import com.google.gwt.dom.client.Style;

import java.io.Serializable;

import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public class GFont implements Serializable {
    public static final Integer DEFAULT_FONT_SIZE = 12;
    public static final String DEFAULT_FONT_FAMILY = "";
    public static final GFont DEFAULT_FONT = new GFont(DEFAULT_FONT_FAMILY, DEFAULT_FONT_SIZE, false, false);
    
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
