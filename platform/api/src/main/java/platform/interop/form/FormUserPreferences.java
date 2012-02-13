package platform.interop.form;

import java.io.Serializable;


public class FormUserPreferences implements Serializable {

    private Boolean needToHide;
    private Integer widthUser;

    public FormUserPreferences(Boolean needToHide, Integer width) {
        this.needToHide = needToHide;
        this.widthUser = width;
    }
    
    public Boolean isNeedToHide(){
        return needToHide;
    }

    public Integer getWidthUser(){
        return widthUser;
    }
}
