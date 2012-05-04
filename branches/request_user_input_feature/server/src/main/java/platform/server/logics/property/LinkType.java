package platform.server.logics.property;

public enum LinkType  {
    DEPEND, EVENTACTION, USEDACTION;

    public final static LinkType[] order = new LinkType[] {DEPEND, EVENTACTION, USEDACTION};
}
