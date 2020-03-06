package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Image;
import lsfusion.gwt.client.base.GwtClientUtils;

public class GImage extends Image {
    public GImage(String imagePath) {
        GwtClientUtils.setSignImage(imagePath, this::setUrl);
    }
}
