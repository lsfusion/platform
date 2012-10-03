package platform.gwt.form2.shared.view.classes;

public class GExcelType extends GFileType {
    public static GExcelType instance = new GExcelType();

    @Override
    public String getMinimumWidth(int minimumCharWidth) {
        return "15px";
    }
}
