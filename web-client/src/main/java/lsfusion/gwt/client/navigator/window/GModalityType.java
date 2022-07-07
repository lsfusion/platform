package lsfusion.gwt.client.navigator.window;

import java.io.Serializable;

import static lsfusion.gwt.client.navigator.window.GShowType.*;

public class GModalityType implements Serializable {
    public GShowType showType;
    public Integer inComponentId;

    public GModalityType() {
    }

    public GModalityType(GShowType showType, Integer inComponentId) {
        this.showType = showType;
        this.inComponentId = inComponentId;
    }

    public boolean isDocked() {
        return showType == DOCKED;
    }

    public boolean isDockedModal() {
        return showType == DOCKED_MODAL;
    }

    public boolean isModal() {
        return showType != DOCKED && inComponentId == null;
    }

    public void setModal() {
        showType = MODAL;
    }

    public boolean isDialog() {
        return showType == DIALOG_MODAL || showType == EMBEDDED || showType == POPUP;
    }

    public boolean isWindow() {
        return showType == MODAL || showType == DIALOG_MODAL;
    }

    public GWindowFormType getWindowType() {
        if(showType == EMBEDDED)
            return GWindowFormType.EMBEDDED;
        if(showType == POPUP)
            return GWindowFormType.POPUP;

        if(isWindow())
            return GWindowFormType.FLOAT;
        else
            return GWindowFormType.DOCKED;
    }
}
