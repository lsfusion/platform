package lsfusion.gwt.client.base.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeStringMap;

// the bookkeeping behind <Platform name/>: a component renders a host node, and the platform moves its own view into
// it. Five owners do this - a form container's platform-drawn children, a navigator window's buttons, the forms
// window's forms, the log window's messages and the navigator's windows - and they differ only in what a name means and
// what moving costs.
// Everything else is the same every time, and getting it slightly different in each copy is what this exists to stop:
//
//   - a host asked for a view that does not exist YET is remembered, not refused: React renders a host once and never
//     offers the same ref again, so giving up would leave the place empty for good
//   - the FIRST host of a name keeps it, so a second cannot be left silently empty
//   - a placement that can never work says so in the host itself, not only in the console
//   - cleanup is the exact inverse of mount, so React's mount->cleanup->mount cannot strand a view
//   - a host the component resizes is said to the owner: GWT hears nothing of a size CSS gives, and a view that measures
//     itself - a grid, a pivot's chart, a map - would keep the size it had until the page itself is resized
public class PlacedViews {

    // a placement that can never work, in the owner's own words. The page gets the short form - it sits inside the
    // host, where there is no room to explain - and the console the whole reason
    public static class Problem {
        public final String shown;
        public final String logged;

        public Problem(String shown, String logged) {
            this.shown = shown;
            this.logged = logged;
        }

        // said in the host and in the console
        public void show(Element host) {
            GwtClientUtils.showCustomViewError(host, shown);
            GwtClientUtils.logCustomViewError(logged);
        }
    }

    // what only the owner knows: whether a name names anything, how to move the view, where it waits, and where the
    // entry of a name is in what the owner projects
    public interface Views {
        // null if the name is fine
        Problem problem(String name);

        // move the platform's view into the host. false when there is nothing to move YET - the host keeps waiting
        boolean place(String name, Element host);

        // take the view back out of wherever it is
        void park(String name);

        // the host the view is in changed size: the view measures itself again, as when a split's border is dragged
        default void resized(String name) {
        }

        // the entry the name keys in the data the owner projects, which hides the host while it says `hidden`
        // (ReactRoot.Placement.entryOf): the field of that name, unless the owner keeps its entries elsewhere
        default JavaScriptObject entryOf(JavaScriptObject data, String name) {
            return GwtClientUtils.getOwnField(data, name);
        }
    }

    private final Views views;

    // a host comes and goes only through addHost / removeHost, which watch its size while it is here
    private final NativeStringMap<Element> hosts = new NativeStringMap<>();  // where a view was asked for, moved or not
    private final NativeStringMap<Element> filled = new NativeStringMap<>(); // where one actually sits
    // the hosts that asked for a name another host already had
    private final RefusedHosts<String> refused = new RefusedHosts<>();
    // watches the size of the hosts in `hosts`. Made with the first of them: an owner whose component places nothing
    // needs none
    private JavaScriptObject resizes;

    public PlacedViews(Views views) {
        this.views = views;
    }

    // the Placement of a React root whose <Platform>s place only this owner's views: they mount and unmount straight
    // here, and the owner says only what its hosts are dressed in - and, through its views, where the entry of a name
    // is. A form container keys and shows more than that, so its root has a placement of its own
    public ReactRoot.Placement placement(ReactRoot.Host kind) {
        return new ReactRoot.Placement() {
            @Override
            public void mount(String name, Element host, JavaScriptObject row) {
                PlacedViews.this.mount(name, host);
            }

            @Override
            public void unmount(String name, Element host, JavaScriptObject row) {
                PlacedViews.this.unmount(name, host);
            }

            @Override
            public ReactRoot.Host getHost() {
                return kind;
            }

            @Override
            public JavaScriptObject entryOf(JavaScriptObject data, String name) {
                return views.entryOf(data, name);
            }
        };
    }

    public boolean mount(String name, Element host) {
        Problem problem = views.problem(name);
        if (problem != null) {
            problem.show(host);
            return false;
        }

        Element asked = hosts.get(name);
        if (asked != null && asked != host) {
            refused.add(name, host);
            reportDuplicate(name, host);
            return false;
        }

        addHost(name, host);
        tryPlace(name, host);
        return true;
    }

    // true when this host was OURS - the one mount accepted - whether or not it ever got a view. That is the pair of
    // mount's own answer, and it is what an owner needs: a host that was still waiting when the component took it away
    // has stopped asking just as surely as one that was holding the view
    public boolean unmount(String name, Element host) {
        boolean ours = hosts.get(name) == host;
        if (ours) // the component took the host away, so nothing waits for it any more
            removeHost(name);
        else
            refused.forget(name, host);

        if (filled.get(name) == host) {
            filled.remove(name);
            views.park(name);
        } else
            GwtClientUtils.clearCustomViewError(host); // a host that never got a view may be holding the diagnostic instead

        // the view is back in the park by now, so a host that was refused while this one held the name can take it -
        // and takes it whether or not this host ever got the view, since either way the name is free
        if (ours)
            refused.takeOver(name, next -> mount(name, next));

        return ours;
    }

    // said in one place, because the row path of a form container reports the same thing without going through the
    // bookkeeping here: a per-row renderer has one host per ROW, which is not the one-view / one-host ledger below
    public static void reportDuplicate(String name, Element host) {
        GwtClientUtils.showCustomViewError(host, "'" + name + "' is already placed by another <Platform>");
        GwtClientUtils.logCustomViewError("'" + name + "' is placed by more than one <Platform>; the first keeps it");
    }

    // whether a host holds this name - with the view in it, or waiting for it: what the owner tells the server is
    // placed
    public boolean isHeld(String name) {
        return hosts.get(name) != null;
    }

    // whatever exists now goes into the host that has been waiting for it
    public void retryPending() {
        hosts.foreachEntry(this::tryPlace);
    }

    // the template form: the view REPLACED the <Platform:name> element instead of being put inside a host, so there is
    // no host to remember and no cleanup to run - the place is consumed and only comes back when the template is
    // redrawn. It is recorded here all the same, so that parkAll below sees every placement, however it was made
    public boolean consumePlace(String name) {
        if (filled.get(name) != null) // already placed, by either protocol - the first one keeps it
            return false;

        filled.put(name, PLACE_CONSUMED);
        return true;
    }

    // a marker, not a node anyone uses: the map has to hold SOMETHING for a consumed place, and a null value would
    // read as "absent" and let the view be placed a second time
    private static final Element PLACE_CONSUMED = Document.get().createDivElement();

    // everything goes back where it waits, so the next render starts clean without losing a view
    public void parkAll() {
        filled.foreachKey(views::park);
        filled.clear();
    }

    // the whole tree this belonged to is gone: every host and every placement with it
    public void reset() {
        hosts.foreachKey(this::removeHost);
        filled.clear();
        refused.clear();
    }

    // the view this name stood for is no longer placed. `mayReturn` says whether the HOST is kept waiting for it -
    // an element the selection took out of the window, or a child a SHOWIF dropped, comes back, and React never
    // re-offers an unchanged ref, so forgetting the host would leave the place empty for good. A closed form does not
    // come back, and its host must not be reused by whatever takes its name next
    public void viewRemoved(String name, boolean mayReturn) {
        filled.remove(name);
        if (!mayReturn)
            removeHost(name);
    }

    // a host changed size - what the browser says after the layout: the view in it, if one is, is told
    private void resized(Element host) {
        filled.foreachEntry((name, filledHost) -> {
            if (filledHost == host)
                views.resized(name);
        });
    }

    private void addHost(String name, Element host) {
        hosts.put(name, host);
        observe(host);
    }

    private void removeHost(String name) {
        Element host = hosts.remove(name);
        if (host != null)
            unobserve(host);
    }

    private native void observe(Element host)/*-{
        var resizes = this.@lsfusion.gwt.client.base.view.PlacedViews::resizes;
        if (!resizes) {
            var placed = this;
            resizes = this.@lsfusion.gwt.client.base.view.PlacedViews::resizes = new $wnd.ResizeObserver(function (entries) {
                entries.forEach(function (entry) {
                    placed.@lsfusion.gwt.client.base.view.PlacedViews::resized(Lcom/google/gwt/dom/client/Element;)(entry.target);
                });
            });
        }
        resizes.observe(host);
    }-*/;

    private native void unobserve(Element host)/*-{
        this.@lsfusion.gwt.client.base.view.PlacedViews::resizes.unobserve(host);
    }-*/;

    private void tryPlace(String name, Element host) {
        if (filled.get(name) != null) // already sitting there
            return;

        if (views.place(name, host))
            filled.put(name, host);
    }
}
