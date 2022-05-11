package lsfusion.gwt.client.base;

import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.util.HashMap;

public class ImageHolder implements Serializable {
    private HashMap<GColorTheme, ImageDescription> images = new HashMap<>(); 

    public ImageHolder() {
    }

    public void addImage(GColorTheme colorTheme, String url, String disabledUrl, int width, int height) {
        images.put(colorTheme, new ImageDescription(url, disabledUrl, width, height));
    }

    public ImageDescription getImage() {
        return images.get(MainFrame.colorTheme);
    }

}
