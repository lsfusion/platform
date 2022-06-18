package lsfusion.gwt.client.base;

import lsfusion.gwt.client.base.size.GSize;

public class Dimension {
    public GSize width;
    public GSize height;

    public Dimension(GSize width, GSize height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return "Dimension{" +
               "width=" + width +
               ", height=" + height +
               '}';
    }
}
