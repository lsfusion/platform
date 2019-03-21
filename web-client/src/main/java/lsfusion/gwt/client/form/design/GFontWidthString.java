package lsfusion.gwt.client.form.design;

import java.io.Serializable;

import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public class GFontWidthString implements Serializable {
    public GFont font;
    public String widthString;

    public GFontWidthString() {
    }

    public GFontWidthString(GFont font) {
        this(font, null);
    }

    public static final GFontWidthString DEFAULT_FONT = new GFontWidthString(GFont.DEFAULT_FONT);

    public GFontWidthString(GFont font, String widthString) {
        this.font = font;
        this.widthString = widthString;
    }

    transient private int hash;
    transient private boolean hashComputed;

    @Override
    public int hashCode() {
        if (hashComputed) {
            hash = font.hashCode();
            hash = 31 * hash + (widthString != null ? widthString.hashCode() : 0);
            hashComputed = true;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof GFontWidthString)) {
            return false;
        }
        GFontWidthString fontWidthString = (GFontWidthString) obj;
        return font.equals(fontWidthString.font) &&
                nullEquals(widthString, fontWidthString.widthString);
    }
}
