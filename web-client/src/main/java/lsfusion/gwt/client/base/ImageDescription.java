package lsfusion.gwt.client.base;

import java.io.Serializable;

public class ImageDescription implements Serializable {
    public String url; // asser is not null
    public int width = -1;
    public int height = -1;

    public ImageDescription() {
    }

    public ImageDescription(String url, int width, int height) {
        this.url = url;
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return url;
    }
}
