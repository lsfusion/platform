package platform.gwt.form.shared.view.classes;

public class GTextType extends GDataType {
    public static GTextType instance = new GTextType();

    @Override
    public String getMinimumMask() {
        return "999 999";
    }

    @Override
    public String getPreferredMask() {
        return "9 999 999";
    }
}
