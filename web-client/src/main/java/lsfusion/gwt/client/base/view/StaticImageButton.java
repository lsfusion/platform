package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.base.BaseStaticImage;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.view.ColorThemeChangeListener;

public class StaticImageButton extends ImageButton implements ColorThemeChangeListener {

    public StaticImageButton(String caption, BaseStaticImage baseImage) {
        super(caption, baseImage, false);
    }
}
