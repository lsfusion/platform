package platform.server.data.classes.where;

import platform.base.QuickMap;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.classes.DataClass;

public abstract class AbstractAndClassWhere<K,This extends AbstractAndClassWhere<K,This>> extends QuickMap<K,ClassSet,This> {

    @Override
    public ClassSet get(K key) {
        ClassSet result = super.get(key);
        if(result==null)
            throw new RuntimeException();
        assert result!=null;
        return result;
    }

    public ClassSet getPartial(K key) {
        return super.get(key);
    }

    boolean means(This where) {
        return where.containsAll(getThis());
    }

    protected ClassSet addValue(ClassSet prevValue, ClassSet newValue) {
        ClassSet andValue = prevValue.and(newValue);
        if(andValue.isEmpty())
            return null;
        else
            return andValue;
    }

    protected boolean containsAll(ClassSet who, ClassSet what) {
        return who.containsAll(what);
    }

    protected AbstractAndClassWhere() {
    }

    protected AbstractAndClassWhere(This set) {
        super(set);
    }

    AbstractAndClassWhere(K key, ClassSet classes) {
        add(key, classes);
    }

    protected ClassSet[] newValues(int size) {
        return new ClassSet[size];
    }
}
