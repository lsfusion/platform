package lsfusion.server.classes;

import lsfusion.base.ArrayCombinations;
import lsfusion.base.col.ListFact;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.OrClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.classes.sets.ResolveConcatenateClassSet;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.ClassCanonicalNameUtils;

import java.util.Arrays;

public class ConcatenateClassSet implements ConcreteClass, ValueClassSet  { // если ValueClassSet, то assert что classes тоже ValueClassSet

    private AndClassSet[] classes;

    public ConcatenateClassSet(AndClassSet[] classes) {
        this.classes = classes;
        assert classes.length > 1;
    }

    public ConcatenateClassSet(ValueClassSet[] classes) {
        this((AndClassSet[])classes);
    }

    public AndClassSet get(int i) {
        return classes[i];
    }

    public AndClassSet and(AndClassSet node) {
        ConcatenateClassSet and = (ConcatenateClassSet) node;
        assert and.classes.length == classes.length;

        AndClassSet[] andClasses = new AndClassSet[classes.length];
        for(int i=0;i<classes.length;i++)
            andClasses[i] = classes[i].and(and.classes[i]);

        return new ConcatenateClassSet(andClasses);
    }

    public AndClassSet or(AndClassSet node) {
        ConcatenateClassSet or = (ConcatenateClassSet) node;
        assert or.classes.length == classes.length;

        AndClassSet[] orClasses = new AndClassSet[classes.length];
        for(int i=0;i<classes.length;i++)
            orClasses[i] = classes[i].or(or.classes[i]);

        return new ConcatenateClassSet(orClasses);
    }

    public boolean isEmpty() {
        for(AndClassSet classSet : classes)
            if(classSet.isEmpty())
                return true;
        return false;
    }

    public boolean containsAll(AndClassSet node, boolean implicitCast) {
        if(!(node instanceof ConcatenateClassSet)) return false;

        ConcatenateClassSet concatenate = (ConcatenateClassSet) node;
        assert concatenate.classes.length == classes.length;

        for(int i=0;i<classes.length;i++)
            if(!classes[i].containsAll(concatenate.classes[i], implicitCast))
                return false;
        return true; 
    }

    public OrClassSet getOr() {
        return new OrConcatenateClass(ListFact.toList(classes).toIndexedMap());
    }

    public Type getType() {
        Type[] types = new Type[classes.length];
        for(int i=0;i<classes.length;i++)
            types[i] = classes[i].getType();
        return ConcatenateType.get(types);
    }
    public Stat getTypeStat(boolean forJoin) {
        Stat result = Stat.ONE;
        for (AndClassSet aClass : classes) result = result.mult(aClass.getTypeStat(forJoin));
        return result;
    }

    public ResolveClassSet toResolve() {
        ResolveClassSet[] sets = new ResolveClassSet[classes.length];
        for(int i=0;i<classes.length;i++)
            sets[i] = classes[i].toResolve();
        return new ResolveConcatenateClassSet(sets);
    }

    public boolean inSet(AndClassSet set) {
        return ConcreteCustomClass.inSet(this, set);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ConcatenateClassSet && Arrays.equals(classes, ((ConcatenateClassSet) o).classes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(classes);
    }

    public AndClassSet[] getAnd() {
        AndClassSet[][] ands = new AndClassSet[classes.length][];
        for(int i=0;i<classes.length;i++)
            ands[i] = classes[i].getAnd();
        ArrayCombinations<AndClassSet> combs = new ArrayCombinations<>(ands, AndClassSet.arrayInstancer);
        if(combs.max==1)
            return new AndClassSet[]{this};
        AndClassSet[] result = new AndClassSet[combs.max]; int k=0;
        for(AndClassSet[] comb : combs)
            result[k++] = new ConcatenateClassSet(comb);
        return result;
    }

    public ValueClassSet getValueClassSet() {
        ValueClassSet[] types = new ValueClassSet[classes.length];
        for(int i=0;i<classes.length;i++)
            types[i] = classes[i].getValueClassSet();
        return new ConcatenateClassSet(types);
    }

    @Override
    public String getShortName() {
        return toString();
    }
}
