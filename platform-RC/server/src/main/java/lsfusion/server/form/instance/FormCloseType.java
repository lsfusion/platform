package lsfusion.server.form.instance;

import com.google.common.base.Preconditions;

public enum FormCloseType {
    OK, CLOSE, DROP;

    public String asString() {
        switch (this) {
            case OK: return "ok";
            case CLOSE: return "close";
            case DROP: return "drop";
        }
        Preconditions.checkState(false, "can't happen!");
        return null;
    }
}
