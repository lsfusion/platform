package lsfusion.interop.form;

import java.io.Serializable;


public class ColumnUserPreferences implements Serializable {

    public Boolean userHide;
    public Integer userWidth;
    public Integer userOrder;
    public Integer userSort;
    public Boolean userAscendingSort;
    
    public ColumnUserPreferences(ColumnUserPreferences prefs) {
        this(prefs.userHide, prefs.userWidth, prefs.userOrder, prefs.userSort, prefs.userAscendingSort);
    }

    public ColumnUserPreferences(Boolean userHide, Integer width, Integer userOrder,
                                 Integer userSort, Boolean userAscendingSort) {
        this.userHide = userHide;
        this.userWidth = width;
        this.userOrder = userOrder;
        this.userSort = userSort;
        this.userAscendingSort = userAscendingSort;
    }
}
