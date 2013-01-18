package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;

public class GImageType extends GFileType {
    public static GImageType instance = new GImageType();

    @Override
    public int getMaximumPixelWidth(int maximumCharWidth, GFont font) {
        return Integer.MAX_VALUE;
    }
}
