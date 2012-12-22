package platform.gwt.form.shared.view.classes;

public class GCustomDynamicFormatFileType extends GFileType {
    public static GCustomDynamicFormatFileType instance = new GCustomDynamicFormatFileType();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth) {
        return 15;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth) {
        return 15;
    }
}
