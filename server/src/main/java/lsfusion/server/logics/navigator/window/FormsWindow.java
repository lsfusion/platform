package lsfusion.server.logics.navigator.window;

import lsfusion.server.physics.dev.i18n.LocalizedString;

// WINDOW ... FORMS: a window that holds forms - what System.forms is, declared by an application wherever it wants
// one. SHOW ... WINDOW <window> opens into it. It draws one form at a time, asking the form already there to close
// when another arrives, unless it is TABBED - which System.forms is, and usually nothing else is
public class FormsWindow extends AbstractWindow {

    // the window a form opens into when the open names none. Written as the name and not looked up, because the
    // default is substituted where the open is READ, and System.lsf itself opens into it long before it is declared
    public static final String DEFAULT_DOCKED_WINDOW_NAME = "System.forms";

    // NOCLOSE: the displaced form is not closed at all. A delay of 0 closes it at once, which is the default
    public static final int NOCLOSE = -1;
    // the delay reaches the client as a timer, and a browser timeout is a signed 32-bit number of milliseconds
    public static final int MAX_CLOSE_DELAY = 2000000;

    public boolean single; // one form at a time - what a FORMS window does unless it is TABBED, which an
                           // EXTEND WINDOW ... FORMS turns over
    // how long the form a new one displaces is kept before it is closed. It is a delay and not a yes / no, because
    // what it buys is going back: a form the user returns to within it is never closed at all
    public int closeDelay;

    public FormsWindow(String canonicalName, LocalizedString caption, boolean single, int closeDelay) {
        super(canonicalName, caption);
        this.single = single;
        this.closeDelay = closeDelay;
    }

    @Override
    public boolean isSingleForm() {
        return single;
    }

    @Override
    public int getFormCloseDelay() {
        return closeDelay;
    }
}
