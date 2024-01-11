package lsfusion.gwt.client.form.design;

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
