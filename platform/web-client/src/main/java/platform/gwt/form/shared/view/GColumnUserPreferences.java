package platform.gwt.form.shared.view;

import java.io.Serializable;

public class GColumnUserPreferences implements Serializable {
    private Boolean needToHide;
    private Integer widthUser;
    private Integer orderUser;
    private Integer sortUser;
    private Boolean ascendingSortUser;

    @SuppressWarnings("UnusedDeclaration")
    public GColumnUserPreferences() {
    }

    public GColumnUserPreferences(Boolean needToHide, Integer width, Integer orderUser,
                                 Integer sortUser, Boolean ascendingSortUser) {
        this.needToHide = needToHide;
        this.widthUser = width;
        this.orderUser = orderUser;
        this.sortUser = sortUser;
        this.ascendingSortUser = ascendingSortUser;
    }

    public Boolean isNeedToHide() {
        return needToHide;
    }

    public Integer getWidthUser() {
        return widthUser;
    }

    public Integer getOrderUser() {
        return orderUser;
    }

    public Integer getSortUser() {
        return sortUser;
    }

    public Boolean getAscendingSortUser() {
        return ascendingSortUser;
    }
}
