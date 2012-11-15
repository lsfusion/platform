package platform.gwt.form.shared.view.classes;

public class GExcelType extends GFileType {
    public static GExcelType instance = new GExcelType();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth) {
        return 15;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth) {
        return 15;
    }
}
