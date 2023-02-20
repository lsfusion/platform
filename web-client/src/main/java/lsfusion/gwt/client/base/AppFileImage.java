package lsfusion.gwt.client.base;

import static lsfusion.gwt.client.base.GwtClientUtils.getAppDownloadURL;

public class AppFileImage implements BaseImage, AppBaseImage {

    public String path;

    public AppFileImage() {
    }

    public AppFileImage(String path) {
        this.path = path;
    }


    @Override
    public String getImageElementSrc(boolean enabled) {
        return getAppDownloadURL(path);
    }
}
