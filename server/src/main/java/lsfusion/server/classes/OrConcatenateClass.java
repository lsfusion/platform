package lsfusion.server.classes;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.OrClassSet;
import lsfusion.server.data.where.classes.AbstractClassWhere;

// в общем случае вместо ConcatenateClassSet может быть просто AndClassSet, но так как Object'ы и DataClass'ы по другому обрабатываются то здесь так
public class OrConcatenateClass extends AbstractClassWhere<Integer,OrConcatenateClass> implements OrClassSet {

    public OrConcatenateClass(And<Integer>[] wheres) {
        super(wheres);
    }

    public OrConcatenateClass(ImMap<Integer, AndClassSet> map) {
        super(map);
    }

    protected OrConcatenateClass createThis(And<Integer>[] wheres) {
        return new OrConcatenateClass(wheres);
    }

    public OrClassSet or(OrClassSet node) {
        return or((OrConcatenateClass)node);
    }

    public OrClassSet and(OrClassSet node) {
        return and((OrConcatenateClass)node);
    }

    public boolean containsAll(OrClassSet node, boolean implicitCast) {
        return ((OrConcatenateClass)node).means(this, implicitCast);
    }

    public ValueClass getCommonClass() {
        assert !isFalse();

        int size = wheres[0].size();
        return new ConcatenateValueClass(ListFact.fromIndexedMap(getCommonParent(
                ListFact.consecutiveList(size, 0).toOrderExclSet().getSet())).toArray(new ValueClass[size]));
    }

    protected OrConcatenateClass FALSETHIS() {
        throw new RuntimeException("not supported");
    }
}
