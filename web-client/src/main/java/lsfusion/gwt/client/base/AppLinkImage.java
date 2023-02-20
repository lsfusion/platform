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
}
