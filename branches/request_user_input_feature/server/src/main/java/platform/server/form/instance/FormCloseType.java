package platform.server.form.instance;

import com.google.common.base.Preconditions;

public enum FormCloseType {
    OK, CLOSE, NULL;

    public String asString() {
        switch (this) {
            case OK: return "ok";
            case CLOSE: return "close";
            case NULL: return "null";
        }
        Preconditions.checkState(false, "can't happen!");
        return null;
    }
}
