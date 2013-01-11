package platform.gwt.form.shared.view.classes;

public class GWordType extends GFileType {
    public static GWordType instance = new GWordType();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, Integer fontSize) {
        return 15;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, Integer fontSize) {
        return 15;
    }
}
