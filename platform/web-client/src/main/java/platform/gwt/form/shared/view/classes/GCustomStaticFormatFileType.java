package platform.gwt.form.shared.view.classes;

public class GCustomStaticFormatFileType extends GFileType {
    public static GCustomStaticFormatFileType instance = new GCustomStaticFormatFileType();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, Integer fontSize) {
        return 15;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, Integer fontSize) {
        return 15;
    }
}
