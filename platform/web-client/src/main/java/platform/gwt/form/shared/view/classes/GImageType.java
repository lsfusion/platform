package platform.gwt.form.shared.view.classes;

public class GImageType extends GFileType {
    public static GImageType instance = new GImageType();

    @Override
    public int getMaximumPixelWidth(int maximumCharWidth, Integer fontSize) {
        return Integer.MAX_VALUE;
    }
}
