package platform.gwt.form2.shared.view.classes;

public class GPDFType extends GFileType {
    public static GPDFType instance = new GPDFType();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth) {
        return 15;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth) {
        return 15;
    }
}
