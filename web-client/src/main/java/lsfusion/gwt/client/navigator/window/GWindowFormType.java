package lsfusion.gwt.client.navigator.window;

import java.io.Serializable;

public interface GWindowFormType extends Serializable {

    default boolean isFloat() {
        return false;
    }

    default boolean isDocked() {
        return false;
    }

    default boolean isEmbedded() {
        return false;
    }

    default boolean isPopup() {
        return false;
    }

    default boolean isEditing() {
        return false;
    }
}
