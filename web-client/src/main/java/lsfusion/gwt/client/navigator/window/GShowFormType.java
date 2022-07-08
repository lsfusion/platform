package lsfusion.gwt.client.navigator.window;

import java.io.Serializable;

public interface GShowFormType extends Serializable {

    default Integer getInContainerId() {
        return null;
    }

    default boolean isDocked() {
        return false;
    }

    default boolean isDockedModal() {
        return false;
    }

    default boolean isModal() {
        return false;
    }

    default boolean isDialog() {
        return false;
    }

    default boolean isWindow() {
        return false;
    }

    GWindowFormType getWindowType();
}