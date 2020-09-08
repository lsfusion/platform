package lsfusion.gwt.client.base.jsni;

public interface HasNativeSID {

    // in group object and property draw sID could be used, but the problem that now they are not guaranteed to be unique
    String getNativeSID(); // should be very fast (reading a field)
}
