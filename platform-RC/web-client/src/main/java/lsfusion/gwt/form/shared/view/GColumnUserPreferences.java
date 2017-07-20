package lsfusion.gwt.form.shared.view;

import java.io.Serializable;

public class GColumnUserPreferences implements Serializable {
    public Boolean userHide;
    public String userCaption;
    public String userPattern;
    public Integer userWidth;
    public Integer userOrder;
    public Integer userSort;
    public Boolean userAscendingSort;

    @SuppressWarnings("UnusedDeclaration")
    public GColumnUserPreferences() {
    }

    public GColumnUserPreferences(GColumnUserPreferences prefs) {
        this(prefs.userHide, prefs.userCaption, prefs.userPattern, prefs.userWidth, prefs.userOrder, prefs.userSort, prefs.userAscendingSort);
    }

    public GColumnUserPreferences(Boolean userHide, String userCaption, String userPattern, Integer width, Integer userOrder,
                                 Integer userSort, Boolean userAscendingSort) {
        this.userHide = userHide;
        this.userCaption = userCaption;
        this.userPattern = userPattern;
        this.userWidth = width;
        this.userOrder = userOrder;
        this.userSort = userSort;
        this.userAscendingSort = userAscendingSort;
    }
}
