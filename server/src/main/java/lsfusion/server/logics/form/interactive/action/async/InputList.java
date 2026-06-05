package lsfusion.server.logics.form.interactive.action.async;

import java.io.Serializable;

public class InputList implements Serializable {

    public final boolean strict;
    public final boolean disableInputList; // the list property disables its inline value list (the dedicated object-id input cast) - editing opens the picker dialog

    public InputList(boolean strict, boolean disableInputList) {
        this.strict = strict;
        this.disableInputList = strict && disableInputList; // disabling the inline list only applies to a strict (object) input
    }
}
