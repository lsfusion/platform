package platform.server.data.where;

public enum FollowDeep {
    PLAIN, INNER, PACK;

    public static FollowDeep inner(boolean pack) {
        return pack?PACK:INNER;
    }

    public FollowDeep max(FollowDeep followDeep) {
        if(followDeep.equals(PACK))
            return PACK;
        else
            if(followDeep.equals(INNER) && equals(PLAIN))
                return PLAIN;
            else
                return this;
    }
}
