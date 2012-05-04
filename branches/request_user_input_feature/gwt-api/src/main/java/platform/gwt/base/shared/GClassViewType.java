package platform.gwt.base.shared;

import java.util.ArrayList;
import java.util.List;

public enum GClassViewType {
    PANEL, GRID, HIDE;

    public static List typeNameList(){
        List list = new ArrayList();
        for(int i=0; i < GClassViewType.values().length; i++){
            list.add(GClassViewType.values()[i].toString());
        }
        return list;
    }

    public static GClassViewType switchView(GClassViewType initClassView) {
        if (initClassView == GRID)
            return PANEL;
        else
            return GRID;
    }
}
