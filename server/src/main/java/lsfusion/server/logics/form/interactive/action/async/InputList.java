package lsfusion.server.logics.form.interactive.action.async;

import java.io.Serializable;

public class InputList implements Serializable {

    public final boolean strict;

    public InputList(boolean strict) {
        this.strict = strict;
    }
}
