package lsfusion.gwt.form.shared.view;

import java.io.Serializable;

public class GFont implements Serializable {
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
}
