package platform.server.data.where;

public enum FollowDeep {
    PLAIN, INNER, PACK;

    public static FollowDeep inner(boolean pack) {
        return pack?PACK:INNER;
    }
}
