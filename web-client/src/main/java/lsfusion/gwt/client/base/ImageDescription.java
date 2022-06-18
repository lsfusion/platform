package lsfusion.gwt.client.base;

import lsfusion.gwt.client.base.size.GSize;

import java.io.Serializable;

public class ImageDescription implements Serializable {
    public String url; // asser is not null
    public String disabledUrl;
    public int width = -1;
    public int height = -1;

    public GSize getWidth() {
        if(width == -1)
            return null;
        return GSize.getImageSize(width);
    }
    public GSize getHeight() {
        if(height == -1)
            return null;
        return GSize.getImageSize(height);
    }

    public ImageDescription() {
    }

    public ImageDescription(String url, String disabledUrl, int width, int height) {
        this.url = url;
        this.disabledUrl = disabledUrl;
        this.width = width;
        this.height = height;
    }

    public String getUrl() {
        return getUrl(true);
    }
    public String getUrl(boolean enabled) {
        return enabled ? url : disabledUrl;
    }

    @Override
    public String toString() {
        return url;
    }
}
