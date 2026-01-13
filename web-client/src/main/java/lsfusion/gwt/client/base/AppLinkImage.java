package lsfusion.gwt.client.base;

public class AppLinkImage implements BaseImage {

    public String url;
    public String extension;

    public AppLinkImage() {
    }

    public AppLinkImage(String url, String extension) {
        this.url = url;
        this.extension = extension;
    }

    @Override
    public String getImageElementSrc(boolean enabled) {
        return url;
    }

    @Override
    public String getTag() {
        return getTag(extension);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AppLinkImage && GwtClientUtils.nullEquals(url, ((AppLinkImage) o).url)
                && GwtClientUtils.nullEquals(extension, ((AppLinkImage) o).extension);
    }

    @Override
    public int hashCode() {
        return GwtClientUtils.nullHash(url) * 31 + GwtClientUtils.nullHash(extension);
    }
}
