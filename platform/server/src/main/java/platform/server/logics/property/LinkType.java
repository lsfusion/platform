package platform.server.logics.property;

public enum LinkType  {
    DEPEND, ACTIONDERIVED, ACTIONUSED;

    public final static LinkType[] order = new LinkType[] {DEPEND, ACTIONDERIVED, ACTIONUSED};
}
