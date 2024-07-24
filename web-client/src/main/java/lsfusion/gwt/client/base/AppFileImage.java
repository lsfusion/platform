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
    public boolean isPDF() {
        return extension != null && extension.equalsIgnoreCase("pdf");
    }

    @Override
    public String getImageElementSrc(boolean enabled) {
        return getAppDownloadURL(path);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AppFileImage && path.equals(((AppFileImage) o).path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
