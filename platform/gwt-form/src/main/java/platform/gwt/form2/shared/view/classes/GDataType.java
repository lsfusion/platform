package platform.gwt.form2.shared.view.classes;

public abstract class GDataType extends GType implements GClass {
    @Override
    public boolean hasChildren() {
        return false;
    }

    public String getMinimumMask() {
        return getPreferredMask();
    }

    public abstract String getPreferredMask();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth) {
        return (minimumCharWidth > 0 ? minimumCharWidth : getMinimumMask().length()) * 7;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth) {
        return (preferredCharWidth > 0 ? preferredCharWidth : getPreferredMask().length()) * 7;
    }
}
