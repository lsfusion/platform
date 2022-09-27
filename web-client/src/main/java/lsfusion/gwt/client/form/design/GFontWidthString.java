package lsfusion.gwt.client.form.design;

import lsfusion.gwt.client.base.GwtClientUtils;

import java.io.Serializable;

public class GFontWidthString implements Serializable {
    public GFont font;
    public String sampleString;

    public GFontWidthString() {
    }

    public GFontWidthString(GFont font, String sampleString) {
        this.font = font;
        this.sampleString = sampleString;
    }

    transient private int hash;
    transient private boolean hashComputed;

    @Override
    public int hashCode() {
        if (hashComputed) {
            hash = font != null ? font.hashCode() : null;
            hash = 31 * hash + sampleString.hashCode();
            hashComputed = true;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GFontWidthString)) {
            return false;
        }
        GFontWidthString fontWidthString = (GFontWidthString) obj;
        return GwtClientUtils.nullEquals(font, fontWidthString.font) &&
                sampleString.equals(fontWidthString.sampleString);
    }
}
