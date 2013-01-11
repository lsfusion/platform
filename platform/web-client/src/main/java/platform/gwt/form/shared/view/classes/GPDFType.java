package platform.gwt.form.shared.view.classes;

public class GPDFType extends GFileType {
    public static GPDFType instance = new GPDFType();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, Integer fontSize) {
        return 15;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, Integer fontSize) {
        return 15;
    }
}
