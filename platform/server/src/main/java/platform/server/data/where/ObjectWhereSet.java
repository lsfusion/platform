package platform.server.data.where;

class ObjectWhereSet {
    DataWhereSet data;
    DataWhereSet not;

    DataWhereSet followData;
    DataWhereSet followNot;

    ObjectWhereSet() {
        data = new DataWhereSet();
        not = new DataWhereSet();

        followData = new DataWhereSet();
        followNot = new DataWhereSet();
    }

    ObjectWhereSet(ObjectWhereSet set) {
        data = new DataWhereSet(set.data);
        not = new DataWhereSet(set.not);

        followData = new DataWhereSet(set.followData);
        followNot = new DataWhereSet(set.followNot);
    }

    ObjectWhereSet(DataWhere where) {
        data = new DataWhereSet();
        data.add(where);
        not = new DataWhereSet();

        followData = new DataWhereSet(where.getFollows());
        followNot = new DataWhereSet();
    }

    ObjectWhereSet(NotWhere where) {
        data = new DataWhereSet();
        not = new DataWhereSet();
        not.add(where.where);

        followData = new DataWhereSet();
        followNot = new DataWhereSet(where.where.getFollows());
    }

    void addAll(ObjectWhereSet set) {
        data.addAll(set.data);
        not.addAll(set.not);

        followData.addAll(set.followData);
        followNot.addAll(set.followNot);
    }

    boolean depends(ObjectWhereSet set) {
        return set.data.intersect(followData) || set.data.intersect(followNot) || data.intersect(set.followNot) || not.intersect(set.followNot);
    }

    // хоть один object A V object B ==TRUE
    boolean linked(ObjectWhereSet set) {
        return set.data.intersect(followNot) || data.intersect(set.followNot);
    }
}
