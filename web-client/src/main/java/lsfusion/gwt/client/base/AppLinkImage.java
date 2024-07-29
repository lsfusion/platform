package lsfusion.gwt.client.base;

public class AppLinkImage implements BaseImage {

    public String url;

    public AppLinkImage() {
    }

    public AppLinkImage(String url) {
        this.url = url;
    }

    @Override
    public String getImageElementSrc(boolean enabled) {
        return url;
    }

    @Override
    public String getTag() {
        return getTag(GwtClientUtils.getFileExtension(url));
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AppLinkImage && url.equals(((AppLinkImage) o).url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
