package lsfusion.interop;

import java.util.ArrayList;
import java.util.List;

public enum ClassViewType {
    PANEL, TOOLBAR, GRID;

    public static ClassViewType[] getAllTypes() {
        return ClassViewType.values();        
    }

    public static ClassViewType switchView(ClassViewType initClassView) {
        if (initClassView.isGrid())
            return PANEL;
        else
            return GRID;
    }
    
    public static ClassViewType DEFAULT = GRID;
    
    public boolean isPanel() {
        return this == PANEL || this == TOOLBAR;
    }

    public boolean isToolbar() {
        return this == TOOLBAR;
    }

    public boolean isGrid() {
        return this == GRID;
    }
}
