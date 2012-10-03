package platform.gwt.form2.shared.view.classes;

public class GPDFType extends GFileType {
    public static GPDFType instance = new GPDFType();

    @Override
    public String getMinimumWidth(int minimumCharWidth) {
        return "15px";
    }
}
