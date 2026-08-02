package lsfusion.gwt.client.base.log;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.base.view.PlacedViews;
import lsfusion.gwt.client.base.view.ReactRoot;
import lsfusion.gwt.client.base.view.RecentlyEventClassHandler;
import lsfusion.gwt.client.view.MainFrame;

// WINDOW log CUSTOM 'MessageLog': a React component draws the window the messages to the user appear in - its own list,
// a counter, the last message only, a toast strip. It never draws a MESSAGE: a message is markup the platform built -
// the text with its icon, or the table an action reported beside it - so the component renders a host per message it
// wants shown, and the platform moves that markup in, the same crossing <Lsf name/> makes for a design child, a
// navigator element and an open form.
//
// A message the component places nowhere is still logged and still in the projection: nothing here ever drops one,
// exactly as the standard panel keeps every message it printed. There is no clear and no dismiss, in the projection or
// in the controller, because there is none in the platform either.
public class ReactLogPanel extends LogPanel {

    // the window keeps the navbar wrapper the standard panel has - the component draws INSIDE it, not instead of it:
    // the log window is hidden, popped out over the form and revealed on a new message by CSS written against
    // .navbar > .navbar-nav, and that is how the pin mode toggled from anywhere else in the client reaches this window
    private final ReactRoot root;

    private final Runnable togglePinMode;

    private final RecentlyEventClassHandler recentlySelected;

    // where a message's markup lives while nothing places it. It has to stay in the document and stay a logical child
    // of the panel, or GWT would detach the widget; the node itself must be OUTSIDE the panel, because React clears
    // its own root
    private final Element park = Document.get().createDivElement();

    private final NativeStringMap<Widget> messages = new NativeStringMap<>();

    private final PlacedViews placed = new PlacedViews(new PlacedViews.Views() {
        @Override
        public PlacedViews.Problem problem(String name) {
            return messages.get(name) != null ? null : new PlacedViews.Problem("'" + name + "' is not a logged message",
                    "'" + name + "' is not a logged message, so nothing will ever be placed here");
        }

        @Override
        public boolean place(String name, Element host) {
            // there is no "not yet" here, which is why nothing retries: an id is handed out only in the projection of
            // a message that has already been printed, and a message is never taken back
            host.appendChild(messages.get(name).getElement());
            return true;
        }

        @Override
        public void park(String name) {
            park.appendChild(messages.get(name).getElement()); // still attached and alive, ready to be placed again
        }
    });

    // the name a component addresses a message by - the one it writes in <Lsf name/>. It is the platform's, not the
    // message's: two messages can carry the same text, caption and second, so nothing about a message identifies it
    private static int messageCounter;

    // the projection, kept between messages because it is built by adding to the previous one - see addMessage
    private JavaScriptObject data = createData();

    public ReactLogPanel(String custom, Runnable togglePinMode) {
        this.togglePinMode = togglePinMode;

        GwtClientUtils.addClassName(panel, "panel-react");

        // the crossing BACK to the platform: the component renders the host node, we move the message's markup in
        root = new ReactRoot(custom, createController(), new ReactRoot.Placement() {
            @Override
            public void mount(String name, Element host, JavaScriptObject row) {
                placed.mount(name, host);
            }

            @Override
            public void unmount(String name, Element host, JavaScriptObject row) {
                placed.unmount(name, host);
            }
        });
        root.updateData(data); // starts empty rather than absent, so the first render already reads data.messages

        park.getStyle().setDisplay(Style.Display.NONE);
        RootPanel.getBodyElement().appendChild(park);

        recentlySelected = new RecentlyEventClassHandler(panel, true, "parent-was-selected-recently", 2000);

        panel.addAttachHandler(event -> {
            if (event.isAttached())
                root.mount(panel.getElement());
            else
                root.unmount();
        });
    }

    @Override
    public void printMessage(Widget message, String caption, boolean failed) {
        String name = "message" + messageCounter++; // prefixed, so it cannot match a form's name by accident

        messages.put(name, message);
        panel.addToElement(message, park); // a logical child of the panel, parked in the DOM until the component says
                                           // where it goes
        // the errorLogMessage / successLogMessage class the standard panel writes onto the markup is not written here:
        // it is that panel's own styling of its own list, and the component styles its list from `failed`. The classes
        // the message was BUILT with (alert, alert-info / alert-danger) are part of the markup and come along with it

        data = addMessage(data, name, caption, failed, System.currentTimeMillis());
        root.updateData(data);

        if (MainFrame.enableShowingRecentlyLogMessages)
            recentlySelected.onEvent(); // the same reveal the standard panel does, and it is the WINDOW being revealed
    }

    // what the component gets as props.controller: the one thing only the platform can do to the log. Pinning is a
    // server action - it is the display environment's setting, shared with the desktop client - so the component
    // cannot run it itself, and it is not told the current mode either, because the client is not: an unpinned window
    // is unpinned by a class the server computes into the window, and there is no boolean anywhere to project
    private native JavaScriptObject createController()/*-{
        var logPanel = this;
        return {
            togglePin: function() {
                logPanel.@lsfusion.gwt.client.base.log.ReactLogPanel::togglePin()();
            }
        };
    }-*/;

    public void togglePin() {
        togglePinMode.run();
    }

    private native JavaScriptObject createData()/*-{
        return { messages: [], byName: {} };
    }-*/;

    // a message is markup, so what is projected about one is only what the standard panel writes ITSELF beside that
    // markup: the caption and the time it prints on the message's date line, and whether the message failed - the
    // panel's only distinction between two messages, and the one the component styles from.
    // `name` is on the entry too, so an entry passed around alone still knows which message it is - `messages` is an
    // array of names, newest first, which is the order the standard panel shows them in
    private native JavaScriptObject addMessage(JavaScriptObject prev, String name, String caption, boolean failed, double time)/*-{
        // a NEW snapshot each time: the store hands the same object to every subscriber and React compares it by
        // reference. The entries are reused, so a component memoized on a message it has already drawn does not redraw
        var byName = {};
        for (var key in prev.byName)
            byName[key] = prev.byName[key];
        byName[name] = { name: name, caption: caption, failed: failed, time: time };

        return { messages: [name].concat(prev.messages), byName: byName };
    }-*/;
}
