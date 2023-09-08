package lsfusion.gwt.client.base;

import static lsfusion.gwt.client.view.MainFrame.useBootstrap;

public abstract class BaseStaticImage implements BaseImage {

    protected String fontClasses;

    public BaseStaticImage() {
    }

    public BaseStaticImage(String fontClasses) {
        this.fontClasses = fontClasses;
    }

    @Override
    public boolean useIcon() {
        return useBootstrap && getFontClasses() != null;
    }

    public String getFontClasses() {
        return fontClasses;
    }
}
