package platform.gwt.form.shared.view;

import java.io.Serializable;

public class ImageDescription implements Serializable {
    public String url;
    public int width = -1;
    public int height = -1;

    public ImageDescription() {
    }

    public ImageDescription(String url) {
        this.url = url;
    }

    public ImageDescription(String url, int width, int height) {
        this(url);
        this.width = width;
        this.height = height;
    }
}
