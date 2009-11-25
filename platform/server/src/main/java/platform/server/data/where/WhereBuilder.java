package platform.server.data.where;

// mutable where чтобы передавать в параметры
public class WhereBuilder {

    private Where where = Where.FALSE;

    public void add(Where add) {
        where = where.or(add);
    }

    public Where toWhere() {
        return where;
    }
}
