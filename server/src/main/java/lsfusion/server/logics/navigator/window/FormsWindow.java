package lsfusion.server.logics.navigator.window;

import lsfusion.server.physics.dev.i18n.LocalizedString;

// WINDOW ... FORMS: a window that holds forms - what System.forms is, declared by an application wherever it wants
// one. SHOW ... DOCKED <window> opens into it. It draws one form at a time, asking the form already there to close
// when another arrives, unless it is TABBED - which System.forms is, and usually nothing else is
public class FormsWindow extends AbstractWindow {

    // the window a form opens into when DOCKED names none. Written as the name and not looked up, because the default
    // is substituted where DOCKED is READ, and System.lsf itself has a bare DOCKED long before it declares this window
    public static final String DEFAULT_DOCKED_WINDOW_NAME = "System.forms";

    public boolean single; // one form at a time - what a FORMS window does unless it is TABBED, which an
                           // EXTEND WINDOW ... FORMS turns over

    public FormsWindow(String canonicalName, LocalizedString caption, boolean single) {
        super(canonicalName, caption);
        this.single = single;
    }

    @Override
    public boolean isSingleForm() {
        return single;
    }
}
