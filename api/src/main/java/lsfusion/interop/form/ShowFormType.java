package lsfusion.interop.form;

import java.io.Serializable;

public interface ShowFormType extends Serializable {

    default Integer getInContainerId() {
        return null;
    }

    default boolean isDockedModal() {
        return false;
    }

    default boolean isModal() {
        return false;
    }

    default boolean isWindow() {
        return false;
    }

    default boolean isDialog() {
        return false;
    }

    WindowFormType getWindowType();

    String getName();

}