package platform.server.classes;

import platform.base.col.interfaces.mutable.MSet;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.ObjectClassSet;
import platform.server.classes.sets.OrObjectClassSet;
import platform.server.classes.sets.UpClassSet;
import platform.server.data.expr.query.Stat;
import platform.server.logics.DataObject;

public abstract class ConcreteCustomClass extends CustomClass implements ConcreteValueClass,ConcreteObjectClass, ObjectValueClassSet {

    public ConcreteCustomClass(String sID, String caption, CustomClass... parents) {
        super(sID, caption, parents);
    }

    public boolean inSet(AndClassSet set) {
        return set.containsAll(this);
    }

    public void fillNextConcreteChilds(MSet<ConcreteCustomClass> mClassSet) {
        mClassSet.add(this);
    }

    public DataObject getClassObject() {
        return new DataObject(ID, getBaseClass().objectClass);
    }

    public OrObjectClassSet getOr() {
        return new OrObjectClassSet(this);
    }

    public String getWhereString(String source) {
        return source + "=" + ID;
    }

    public String getNotWhereString(String source) {
        return source + " IS NULL OR NOT " + getWhereString(source);
    }

    public ObjectClassSet and(AndClassSet node) {
        return and(this,node);
    }

    public AndClassSet or(AndClassSet node) {
        return or(this,node); 
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean containsAll(AndClassSet node) {
        return node instanceof ConcreteCustomClass && this==node;
    }

    public ConcreteCustomClass getSingleClass() {
        if(children.isEmpty())
            return this;
        else
            return null;
    }

    // мн-ое наследование для ConcreteObjectClass
    public static ObjectClassSet and(ConcreteObjectClass set1, AndClassSet set2) {
        return set1.inSet(set2)?set1:UpClassSet.FALSE;
    }
    public static AndClassSet or(ConcreteObjectClass set1, AndClassSet set2) {
        return set1.inSet(set2)?set2:OrObjectClassSet.or(set1,set2); 
    }

    public AndClassSet getKeepClass() {
        return getBaseClass().getUpSet();
    }

    public Stat getStat() {
        return new Stat(getCount());
    }

    public AndClassSet[] getAnd() {
        return new AndClassSet[]{this};
    }
}
