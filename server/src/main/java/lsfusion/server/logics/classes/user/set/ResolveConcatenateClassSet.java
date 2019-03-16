package lsfusion.server.logics.classes.user.set;

import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.struct.ConcatenateClassSet;
import lsfusion.server.logics.classes.struct.ConcatenateValueClass;
import lsfusion.server.physics.dev.id.name.ClassCanonicalNameUtils;

import java.util.Arrays;

public class ResolveConcatenateClassSet implements ResolveClassSet {

    private ResolveClassSet[] classes;

    public ResolveConcatenateClassSet(ResolveClassSet[] classes) {
        this.classes = classes;
        assert classes.length > 1;
    }

    public ResolveClassSet get(int i) {
        return classes[i];
    }

    public ResolveClassSet and(ResolveClassSet node) {
        if(!(node instanceof ResolveConcatenateClassSet))
            return ResolveUpClassSet.FALSE;

        ResolveConcatenateClassSet and = (ResolveConcatenateClassSet) node;
        assert and.classes.length == classes.length;

        ResolveClassSet[] andClasses = new ResolveClassSet[classes.length];
        for(int i=0;i<classes.length;i++)
            andClasses[i] = classes[i].and(and.classes[i]);

        return new ResolveConcatenateClassSet(andClasses);
    }

    public ResolveClassSet or(ResolveClassSet node) {
        ResolveConcatenateClassSet or = (ResolveConcatenateClassSet) node;
        assert or.classes.length == classes.length;

        ResolveClassSet[] orClasses = new ResolveClassSet[classes.length];
        for(int i=0;i<classes.length;i++)
            orClasses[i] = classes[i].or(or.classes[i]);

        return new ResolveConcatenateClassSet(orClasses);
    }

    public boolean isEmpty() {
        for(ResolveClassSet classSet : classes)
            if(classSet.isEmpty())
                return true;
        return false;
    }

    public boolean containsAll(ResolveClassSet node, boolean implicitCast) {
        if(!(node instanceof ResolveConcatenateClassSet)) return false;

        ResolveConcatenateClassSet concatenate = (ResolveConcatenateClassSet) node;
        assert concatenate.classes.length == classes.length;

        for(int i=0;i<classes.length;i++)
            if(!classes[i].containsAll(concatenate.classes[i], implicitCast))
                return false;
        return true;
    }

    @Override
    public boolean equalsCompatible(ResolveClassSet set) {
        if(!(set instanceof ResolveConcatenateClassSet)) return false;

        ResolveConcatenateClassSet concatenate = (ResolveConcatenateClassSet) set;
        assert concatenate.classes.length == classes.length;

        for(int i=0;i<classes.length;i++)
            if(!classes[i].equalsCompatible(concatenate.classes[i]))
                return false;
        return true;
    }

    public Type getType() {
        Type[] types = new Type[classes.length];
        for(int i=0;i<classes.length;i++)
            types[i] = classes[i].getType();
        return ConcatenateType.get(types);
    }

    public ValueClass getCommonClass() {
        ValueClass[] types = new ValueClass[classes.length];
        for(int i=0;i<classes.length;i++)
            types[i] = classes[i].getCommonClass();
        return new ConcatenateValueClass(types);
    }

    public AndClassSet toAnd() {
        AndClassSet[] types = new AndClassSet[classes.length];
        for(int i=0;i<classes.length;i++)
            types[i] = classes[i].toAnd();
        return new ConcatenateClassSet(types);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ResolveConcatenateClassSet && Arrays.equals(classes, ((ResolveConcatenateClassSet) o).classes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(classes);
    }

    public ResolveClassSet[] getClasses() {
        return classes;
    }

    @Override
    public String getCanonicalName() {
        return ClassCanonicalNameUtils.createName(this);
    }
}
