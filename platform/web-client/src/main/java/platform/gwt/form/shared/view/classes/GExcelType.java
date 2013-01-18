package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;

public class GExcelType extends GFileType {
    public static GExcelType instance = new GExcelType();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        return 15;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        return 15;
    }
}
