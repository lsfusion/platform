package lsfusion.server.logics.form.interactive.action.async;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class QuickAccess {
    public static List<QuickAccess> DEFAULT = Arrays.asList(new QuickAccess(QuickAccessMode.SELECTED, true), new QuickAccess(QuickAccessMode.FOCUSED, false));
    public static List<QuickAccess> EMPTY = Collections.emptyList();

    public QuickAccessMode mode;
    public boolean hover;

    public QuickAccess(QuickAccessMode mode, boolean hover) {
        this.mode = mode;
        this.hover = hover;
    }
}