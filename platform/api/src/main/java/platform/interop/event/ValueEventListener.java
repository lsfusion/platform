package platform.interop.event;

import sun.awt.SunHints;

public interface ValueEventListener {

    void actionPerfomed(ValueEvent event);

    String getEventSID();

}
