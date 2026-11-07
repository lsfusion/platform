package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// the hosts that asked for a view another host already had, by the owner's key in PlacedViews - a name, or a row and
// a property. React renders the same <Platform> until its own props change, so a host refused is never offered again
// on its own - and a component that stops rendering the one that WON would leave the view parked with a place for it
// still on the screen. Kept in order: the eldest asks first
public class RefusedHosts<K> {

    private final NativeHashMap<K, List<Element>> hosts = new NativeHashMap<>();

    public void add(K key, Element host) {
        List<Element> asked = hosts.get(key);
        if (asked == null) {
            asked = new ArrayList<>();
            hosts.put(key, asked);
        }
        asked.add(host);
    }

    // a host that asked and was refused, and that the component has now taken away: it is not waiting any more
    public void forget(K key, Element host) {
        List<Element> asked = hosts.get(key);
        if (asked != null && asked.remove(host) && asked.isEmpty())
            hosts.remove(key);
    }

    // the host that had the view is gone: the eldest host that asked for it meanwhile takes it, if it is still in the
    // document - a component that removed both in one render leaves nothing to take over, and a host React has thrown
    // away is not one of ours to fill
    public void takeOver(K key, Consumer<Element> place) {
        List<Element> asked = hosts.get(key);
        if (asked == null)
            return;

        while (!asked.isEmpty()) {
            Element next = asked.remove(0);
            if (GwtClientUtils.isInDocument(next)) {
                GwtClientUtils.clearCustomViewError(next); // it has been holding "already placed by another <Platform>"
                place.accept(next);
                break;
            }
        }
        if (asked.isEmpty())
            hosts.remove(key);
    }

    public void clear() {
        hosts.clear();
    }
}
