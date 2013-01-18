package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;

public class GWordType extends GFileType {
    public static GWordType instance = new GWordType();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        return 15;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        return 15;
    }
}
