package platform.gwt.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GGroupObject implements Serializable {
    public List<GObject> objects = new ArrayList<GObject>();

    public GGrid grid;
    public GShowType showType;
    public int ID;
    public List<String> banClassView;

    public String getCaption() {
        if (objects.isEmpty()) {
            //todo: локализовать попозже через GWT-шный Messages interface
            return "Empty group";
        }

        String result = "";
        for (GObject object : objects) {
            if (!result.isEmpty()) {
                result += ", ";
            }
            result += object.getCaption();
        }
        return result;
    }

}
