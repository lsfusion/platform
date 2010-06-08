package platform.server.classes;

import platform.server.data.expr.ValueExpr;
import platform.server.classes.sets.*;

public class ConcreteCustomClass extends CustomClass implements ConcreteValueClass,ConcreteObjectClass {

    public ConcreteCustomClass(Integer iID, String iCaption, CustomClass... iParents) {
        super(iID, iCaption, iParents);
    }

    public boolean inSet(AndClassSet set) {
        return set.containsAll(this);
    }

    public void fillNextConcreteChilds(ConcreteCustomClassSet classSet) {
        classSet.add(this);            
    }

    public ValueExpr getIDExpr() {
        return new ValueExpr(ID, SystemClass.instance);
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
        return set1.inSet(set2)?set1:OrObjectClassSet.or(set1,set2); 
    }
}
