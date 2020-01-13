package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Image;
import lsfusion.gwt.client.base.Callback;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.base.GwtClientUtils.getModuleImagePath;

public class GImage extends Image {
    public GImage(String imagePath) {
        String colorThemeImagePath = MainFrame.colorTheme.getImagePath(imagePath);
        GwtClientUtils.ensureImage(colorThemeImagePath, new Callback() {
            @Override
            public void onFailure() {
                setUrl(getModuleImagePath(imagePath));
            }

            @Override
            public void onSuccess() {
                setUrl(getModuleImagePath(colorThemeImagePath));
            }
        });
    }
}
