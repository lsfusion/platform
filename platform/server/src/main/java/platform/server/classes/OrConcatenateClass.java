package platform.server.classes;

import platform.server.classes.sets.OrClassSet;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.where.classes.AbstractClassWhere;

import java.util.Map;

// в общем случае вместо ConcatenateClassSet может быть просто AndClassSet, но так как Object'ы и DataClass'ы по другому обрабатываются то здесь так
public class OrConcatenateClass extends AbstractClassWhere<Integer,OrConcatenateClass> implements OrClassSet {

    public OrConcatenateClass(And<Integer>[] wheres) {
        super(wheres);
    }

    public OrConcatenateClass(Map<Integer, AndClassSet> map) {
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

    public boolean containsAll(OrClassSet node) {
        return ((OrConcatenateClass)node).means(this);
    }

    public ValueClass getCommonClass() {
        throw new RuntimeException("not supported yet");
    }
}
