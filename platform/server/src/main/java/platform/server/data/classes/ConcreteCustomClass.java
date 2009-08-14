package platform.server.data.classes;

import platform.server.data.classes.where.*;
import platform.server.data.query.exprs.ValueExpr;

public class ConcreteCustomClass extends CustomClass implements ConcreteValueClass,ConcreteObjectClass {

    public ConcreteCustomClass(Integer iID, String iCaption, CustomClass... iParents) {
        super(iID, iCaption, iParents);
    }

    public boolean inSet(AndClassSet set) {
        return (set instanceof ConcreteCustomClass && equals(set)) || (set instanceof UpClassSet && ((UpClassSet) set).has(this));
    }

    public void fillNextConcreteChilds(ConcreteCustomClassSet classSet) {
        classSet.add(this);            
    }

    public ValueExpr getIDExpr() {
        return new ValueExpr(ID, SystemClass.instance);
    }

    public OrClassSet getOr() {
        return new OrObjectClassSet(this);
    }

    public String getWhereString(String source) {
        return source + "=" + ID;
    }

    public String getNotWhereString(String source) {
        return source + " IS NULL OR NOT " + getWhereString(source);
    }

    public ObjectClassSet and(AndClassSet node) {
        return inSet(node)?this:UpClassSet.FALSE;
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
}
