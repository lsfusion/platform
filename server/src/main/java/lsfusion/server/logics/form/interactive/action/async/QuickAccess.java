package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;

public class QuickAccess {
    public static ImList<QuickAccess> DEFAULT = ListFact.toList(new QuickAccess(QuickAccessMode.SELECTED, true), new QuickAccess(QuickAccessMode.FOCUSED, true));
    public static ImList<QuickAccess> EMPTY = ListFact.EMPTY();

    public QuickAccessMode mode;
    public boolean hover;

    public QuickAccess(QuickAccessMode mode, boolean hover) {
        this.mode = mode;
        this.hover = hover;
    }
}