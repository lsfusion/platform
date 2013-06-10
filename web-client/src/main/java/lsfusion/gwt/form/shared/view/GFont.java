package lsfusion.gwt.form.shared.view;

import lsfusion.gwt.base.shared.GwtSharedUtils;

import java.io.Serializable;

public class GFont implements Serializable {
    public static final String BOLD = "bold";
    public static final String ITALIC = "italic";

    public String style;
    public String weight;
    public Integer size;
    public String family;

    public GFont() {
    }

    public GFont(String style, String weight, Integer size, String family) {
        this.style = style;
        this.weight = weight;
        this.size = size;
        this.family = family;
    }

    public String getFullFont() {
        String font = "";
        if (style != null) {
            font += style + " ";
        }
        if (weight != null) {
            font += weight + " ";
        }
        if (size != null) {
            font += size + "px ";
        }
        if (family != null) {
            font += family;
        }
        return font;
    }

    public boolean isBold() {
        return weight != null && weight.equals(BOLD);
    }

    public boolean equalsForMetrics(GFont font) {
        if (font == null) {
            return false;
        }
        return GwtSharedUtils.nullEquals(style, font.style) && GwtSharedUtils.nullEquals(size, font.size) && GwtSharedUtils.nullEquals(weight, font.weight);
    }
}
