package lsfusion.interop.form.object.table.grid.user.design;

import java.io.Serializable;


public class ColumnUserPreferences implements Serializable {

    public Boolean userHide;
    public String userCaption;
    public String userPattern;
    public Integer userWidth;
    public Double userFlex;
    public Integer userOrder;
    public Integer userSort;
    public Boolean userAscendingSort;
    
    public ColumnUserPreferences(ColumnUserPreferences prefs) {
        this(prefs.userHide, prefs.userCaption, prefs.userPattern, prefs.userWidth, prefs.userFlex, prefs.userOrder, prefs.userSort, prefs.userAscendingSort);
    }

    public ColumnUserPreferences(Boolean userHide, String userCaption, String userPattern, Integer width, Double flex, Integer userOrder,
                                 Integer userSort, Boolean userAscendingSort) {
        this.userHide = userHide;
        this.userCaption = userCaption;
        this.userPattern = userPattern;
        this.userWidth = width;
        this.userFlex = flex;
        this.userOrder = userOrder;
        this.userSort = userSort;
        this.userAscendingSort = userAscendingSort;
    }
}
