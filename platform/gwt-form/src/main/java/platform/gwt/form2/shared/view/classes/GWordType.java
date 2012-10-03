package platform.gwt.form2.shared.view.classes;

public class GWordType extends GFileType {
    public static GWordType instance = new GWordType();

    @Override
    public String getMinimumWidth(int minimumCharWidth) {
        return "15px";
    }
}
