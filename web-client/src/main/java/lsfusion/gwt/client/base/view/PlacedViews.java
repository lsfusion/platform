package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeStringMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// the bookkeeping behind <Lsf name/>: a component renders a host node, and the platform moves its own view into it.
// Four owners do this - a form container's lsf children, a navigator window's buttons, the forms window's forms and the
// log window's messages - and they differ only in what a name means and what moving costs. Everything else is the same every time, and getting it
// slightly different in each copy is what this exists to stop:
//
//   - a host asked for a view that does not exist YET is remembered, not refused: React renders a host once and never
//     offers the same ref again, so giving up would leave the place empty for good
//   - the FIRST host of a name keeps it, so a second cannot be left silently empty
//   - a placement that can never work says so in the host itself, not only in the console
//   - cleanup is the exact inverse of mount, so React's mount->cleanup->mount cannot strand a view
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
    }

    // what only the owner knows: whether a name names anything, how to move the view, and where it waits
    public interface Views {
        // null if the name is fine
        Problem problem(String name);

        // move the platform's view into the host. false when there is nothing to move YET - the host keeps waiting
        boolean place(String name, Element host);

        // take the view back out of wherever it is
        void park(String name);
    }

    private final Views views;

    private final NativeStringMap<Element> hosts = new NativeStringMap<>();  // where a view was asked for, moved or not
    private final NativeStringMap<Element> filled = new NativeStringMap<>(); // where one actually sits
    // the hosts that asked for a name another host already had. React renders the same <Lsf> until its own props
    // change, so a host refused here is never offered again - and a component that stops rendering the one that WON
    // would leave the view parked with a place for it still on the screen. Kept in order: the eldest asks first
    private final Map<String, List<Element>> refused = new HashMap<>();

    public PlacedViews(Views views) {
        this.views = views;
    }

    public boolean mount(String name, Element host) {
        Problem problem = views.problem(name);
        if (problem != null) {
            GwtClientUtils.showLsfViewError(host, problem.shown);
            GwtClientUtils.logLsfViewError(problem.logged);
            return false;
        }

        Element asked = hosts.get(name);
        if (asked != null && asked != host) {
            refused.computeIfAbsent(name, key -> new ArrayList<>()).add(host);
            reportDuplicate(name, host);
            return false;
        }

        hosts.put(name, host);
        tryPlace(name, host);
        return true;
    }

    // true when this host was OURS - the one mount accepted - whether or not it ever got a view. That is the pair of
    // mount's own answer, and it is what an owner needs: a host that was still waiting when the component took it away
    // has stopped asking just as surely as one that was holding the view
    public boolean unmount(String name, Element host) {
        boolean ours = hosts.get(name) == host;
        if (ours) // the component took the host away, so nothing waits for it any more
            hosts.remove(name);
        else
            forget(name, host);

        if (filled.get(name) == host) {
            filled.remove(name);
            views.park(name);
        } else
            GwtClientUtils.clearLsfViewError(host); // a host that never got a view may be holding the diagnostic instead

        // the view is back in the park by now, so a host that was refused while this one held the name can take it -
        // and takes it whether or not this host ever got the view, since either way the name is free
        if (ours)
            takeOver(name);

        return ours;
    }

    // the host that had the name is gone. The eldest host that asked for it meanwhile takes it, if it is still in the
    // document - a component that removed both in one render leaves nothing to take over, and a host React has thrown
    // away is not one of ours to fill
    private void takeOver(String name) {
        List<Element> asked = refused.get(name);
        if (asked == null)
            return;

        while (!asked.isEmpty()) {
            Element next = asked.remove(0);
            if (GwtClientUtils.isInDocument(next)) {
                GwtClientUtils.clearLsfViewError(next); // it has been holding "already placed by another <Lsf>"
                mount(name, next);
                break;
            }
        }
        if (asked.isEmpty())
            refused.remove(name);
    }

    // a host that asked and was refused, and that the component has now taken away: it is not waiting any more
    private void forget(String name, Element host) {
        List<Element> asked = refused.get(name);
        if (asked != null && asked.remove(host) && asked.isEmpty())
            refused.remove(name);
    }


    // said in one place, because the row path of a form container reports the same thing without going through the
    // bookkeeping here: a per-row renderer has one host per ROW, which is not the one-view / one-host ledger below
    public static void reportDuplicate(String name, Element host) {
        GwtClientUtils.showLsfViewError(host, "'" + name + "' is already placed by another <Lsf>");
        GwtClientUtils.logLsfViewError("'" + name + "' is placed by more than one <Lsf>; the first one keeps it");
    }

    // whatever exists now goes into the host that has been waiting for it
    public void retryPending() {
        hosts.foreachEntry(this::tryPlace);
    }

    // the template form: the view REPLACED the <Lsf:name> element instead of being put inside a host, so there is no
    // host to remember and no cleanup to run - the place is consumed and only comes back when the template is redrawn.
    // It is recorded here all the same, so that parkAll below sees every placement, however it was made
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
        hosts.clear();
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
            hosts.remove(name);
    }

    private void tryPlace(String name, Element host) {
        if (filled.get(name) != null) // already sitting there
            return;

        if (views.place(name, host))
            filled.put(name, host);
    }
}
