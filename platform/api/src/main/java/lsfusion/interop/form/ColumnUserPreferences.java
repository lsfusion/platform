package lsfusion.interop.form;

import java.io.Serializable;


public class ColumnUserPreferences implements Serializable {

    private Boolean needToHide;
    private Integer widthUser;
    private Integer orderUser;
    private Integer sortUser;
    private Boolean ascendingSortUser;

    public ColumnUserPreferences(Boolean needToHide, Integer width, Integer orderUser,
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

    public Boolean getAscendingSortUser(){
        return ascendingSortUser;
    }

}
