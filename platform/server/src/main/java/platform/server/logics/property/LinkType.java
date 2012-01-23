package platform.server.logics.property;

import platform.server.logics.BusinessLogics;

public enum LinkType  {
    DEPEND, FOLLOW, CHANGE;

    public boolean less(LinkType linkType) {
        return linkType != CHANGE && this != DEPEND && this != linkType;
    }

    public final static LinkType[] order = new LinkType[] {DEPEND, FOLLOW, CHANGE};
}
