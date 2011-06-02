package platform.gwt.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GGroupObject implements Serializable {
    public List<GObject> objects = new ArrayList<GObject>();

    public GGrid grid;
    public GShowType showType;
    public int ID;

    public String getCaption() {
        if (objects.isEmpty()) {
            return "Пустая группа";
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
