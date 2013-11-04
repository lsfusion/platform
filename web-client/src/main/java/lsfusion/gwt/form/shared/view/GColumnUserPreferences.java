package lsfusion.gwt.form.shared.view;

import java.io.Serializable;

public class GColumnUserPreferences implements Serializable {
    public Boolean userHide;
    public Integer userWidth;
    public Integer userOrder;
    public Integer userSort;
    public Boolean userAscendingSort;

    @SuppressWarnings("UnusedDeclaration")
    public GColumnUserPreferences() {
    }

    public GColumnUserPreferences(GColumnUserPreferences prefs) {
        this(prefs.userHide, prefs.userWidth, prefs.userOrder, prefs.userSort, prefs.userAscendingSort);
    }

    public GColumnUserPreferences(Boolean userHide, Integer width, Integer userOrder,
                                 Integer userSort, Boolean userAscendingSort) {
        this.userHide = userHide;
        this.userWidth = width;
        this.userOrder = userOrder;
        this.userSort = userSort;
        this.userAscendingSort = userAscendingSort;
    }
}
