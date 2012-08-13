package platform.interop.form;

import java.io.Serializable;


public class FormColumnUserPreferences implements Serializable {

    private Boolean needToHide;
    private Integer widthUser;
    private Integer orderUser;

    public FormColumnUserPreferences(Boolean needToHide, Integer width, Integer orderUser) {
        this.needToHide = needToHide;
        this.widthUser = width;
        this.orderUser = orderUser;
    }
    
    public Boolean isNeedToHide(){
        return needToHide;
    }

    public Integer getWidthUser(){
        return widthUser;
    }
    
    public Integer getOrderUser(){
        return orderUser;
    }
}
