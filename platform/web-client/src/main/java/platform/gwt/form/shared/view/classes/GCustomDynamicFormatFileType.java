package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;

public class GCustomDynamicFormatFileType extends GFileType {
    public static GCustomDynamicFormatFileType instance = new GCustomDynamicFormatFileType();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        return 15;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        return 15;
    }
}
