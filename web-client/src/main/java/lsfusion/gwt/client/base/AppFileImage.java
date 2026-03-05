package lsfusion.gwt.client.base;

import static lsfusion.gwt.client.base.GwtClientUtils.getAppDownloadURL;

public class AppFileImage implements BaseImage, AppBaseImage {

    public String path;
    public String extension;

    public AppFileImage() {
    }

    public AppFileImage(String path, String extension) {
        this.path = path;
        this.extension = extension;
    }

    @Override
    public String getTag() {
        return getTag(extension);
    }

    @Override
    public String getImageElementSrc(boolean enabled) {
        return getAppDownloadURL(path);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AppFileImage && GwtClientUtils.nullEquals(path, ((AppFileImage) o).path)
                && GwtClientUtils.nullEquals(extension, ((AppFileImage) o).extension);
    }

    @Override
    public int hashCode() {
        return GwtClientUtils.nullHash(path) * 31 + GwtClientUtils.nullHash(extension);
    }
}
