package lsfusion.interop;

import java.util.ArrayList;
import java.util.List;

public enum ClassViewType {
    PANEL, GRID, HIDE;

    public static List typeNameList(){
        List list = new ArrayList();
        for(int i=0; i < ClassViewType.values().length; i++){
            list.add(ClassViewType.values()[i].toString());
        }
        return list;
    }

    public static ClassViewType switchView(ClassViewType initClassView) {
        if (initClassView == GRID)
            return PANEL;
        else
            return GRID;
    }
}
