package platform.gwt.form.shared.view.classes;

public class GExcelType extends GFileType {
    public static GExcelType instance = new GExcelType();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, Integer fontSize) {
        return 15;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, Integer fontSize) {
        return 15;
    }
}
