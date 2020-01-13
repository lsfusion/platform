package lsfusion.gwt.client.base;

import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.util.HashMap;

public class ImageHolder implements Serializable {
    private HashMap<GColorTheme, ImageDescription> images = new HashMap<>(); 

    public ImageHolder() {
    }

    public void addImage(GColorTheme colorTheme, String url, int width, int height) {
        images.put(colorTheme, new ImageDescription(url, width, height));
    }

    public ImageDescription getImage() {
        return getImage(MainFrame.colorTheme);
    }

    public ImageDescription getImage(GColorTheme colorTheme) {
        ImageDescription themeImage = images.get(colorTheme);
        return themeImage != null ? themeImage : getDefaultImage();
    }
    
    public ImageDescription getDefaultImage() {
        return images.get(GColorTheme.DEFAULT);
    }
}
