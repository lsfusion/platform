package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;

public class GPDFType extends GFileType {
    public static GPDFType instance = new GPDFType();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        return 15;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        return 15;
    }
}
