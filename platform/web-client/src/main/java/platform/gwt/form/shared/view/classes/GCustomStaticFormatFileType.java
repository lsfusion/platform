package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;

public class GCustomStaticFormatFileType extends GFileType {
    public static GCustomStaticFormatFileType instance = new GCustomStaticFormatFileType();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        return 15;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        return 15;
    }
}
