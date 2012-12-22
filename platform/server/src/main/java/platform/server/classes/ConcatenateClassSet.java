package platform.server.classes;

import platform.base.ArrayCombinations;
import platform.base.BaseUtils;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrClassSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.type.ConcatenateType;
import platform.server.data.type.Type;

import java.util.Arrays;

public class ConcatenateClassSet implements ConcreteClass  {

    private AndClassSet[] classes;

    public ConcatenateClassSet(AndClassSet[] classes) {
        this.classes = classes;
        assert classes.length > 1;
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
            if((orClasses[i] = classes[i].or(or.classes[i]))==null)
                return null;

        return new ConcatenateClassSet(orClasses);
    }

    public boolean isEmpty() {
        for(AndClassSet classSet : classes)
            if(classSet.isEmpty())
                return true;
        return false;
    }

    public boolean containsAll(AndClassSet node) {
        if(!(node instanceof ConcatenateClassSet)) return false;

        ConcatenateClassSet concatenate = (ConcatenateClassSet) node;
        assert concatenate.classes.length == classes.length;

        for(int i=0;i<classes.length;i++)
            if(!classes[i].containsAll(concatenate.classes[i]))
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
        return new ConcatenateType(types);
    }
    public Stat getTypeStat() {
        Stat result = Stat.ONE;
        for (AndClassSet aClass : classes) result = result.mult(aClass.getTypeStat());
        return result;
    }

    public boolean inSet(AndClassSet set) {
        return set.containsAll(this);
    }

    public AndClassSet getKeepClass() {
        return this;
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
        ArrayCombinations<AndClassSet> combs = new ArrayCombinations<AndClassSet>(ands, AndClassSet.arrayInstancer);
        if(combs.max==1)
            return new AndClassSet[]{this};
        AndClassSet[] result = new AndClassSet[combs.max]; int k=0;
        for(AndClassSet[] comb : combs)
            result[k++] = new ConcatenateClassSet(comb);
        return result;
    }
}
