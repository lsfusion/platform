package lsfusion.gwt.client.base.log;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.navigator.view.NavigatorPanel;

// where the messages to the user go: the standard panel, or a React component the application named with
// WINDOW log CUSTOM. Which one draws the window is decided once, in GLog.createLogPanel, where the window is known -
// everything else about a message (what it is made of, when it is shown) stays where it was, in GLog and its caller.
//
// It is the log window's view itself, and not something holding one: the window is the navbar both panels draw inside,
// its own classes are written into that navbar as the pin mode changes, and the panel is never swapped out from under
// WindowsController. So there is nothing here for a getView() to return but `this`
public abstract class LogPanel extends NavigatorPanel {

    protected LogPanel() {
        super(true); // the messages go one under another
    }

    // `message` is the platform's own markup for the message - the text with its icon, or the table an action reported
    // beside it - `caption` the caption of the action that logged it, and `failed` whether it is an error
    public abstract void printMessage(Widget message, String caption, boolean failed);
}
