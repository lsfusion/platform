package lsfusion.interop.form;

import java.io.Serializable;

import static lsfusion.interop.form.ShowType.*;

public class ModalityType implements Serializable {
    public ShowType showType;
    public Integer inComponentId;

    public ModalityType(ShowType showType, Integer inComponentId) {
        this.showType = showType;
        this.inComponentId = inComponentId;
    }

    public boolean isDockedModal () {
        return showType == ShowType.DOCKED_MODAL;
    }

    public boolean isModal() {
        return showType != DOCKED && inComponentId == null;
    }

    public void setModal() {
        this.showType = MODAL;
    }

    public boolean isWindow() {
        return showType == MODAL || showType == DIALOG_MODAL;
    }

    public boolean isDialog() {
        return showType == DIALOG_MODAL || showType == EMBEDDED || showType == POPUP;
    }

    public WindowFormType getWindowType() {
        if(showType == EMBEDDED)
            return WindowFormType.EMBEDDED;
        if(showType == POPUP)
            return WindowFormType.POPUP;

        if(isWindow())
            return WindowFormType.FLOAT;
        else
            return WindowFormType.DOCKED;
    }

    public String getName() {
        return showType.name();
    }
}
