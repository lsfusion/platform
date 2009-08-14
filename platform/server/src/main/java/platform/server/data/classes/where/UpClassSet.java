package platform.server.data.classes.where;

import platform.base.SetWhere;
import platform.server.data.classes.*;
import platform.server.data.types.ObjectType;
import platform.server.data.types.Type;

import java.util.HashSet;
import java.util.Set;

// выше вершин
public class UpClassSet extends SetWhere<CustomClass,UpClassSet> implements ObjectClassSet {

    private UpClassSet(CustomClass[] iClasses) {
        super(iClasses);
    }

    public static final UpClassSet FALSE = new UpClassSet(new CustomClass[0]);

    public UpClassSet(CustomClass customClass) {
        this(new CustomClass[]{customClass});
    }

    public BaseClass getBaseClass() {
        return wheres[0].getBaseClass();
    }

    public boolean has(CustomClass checkNode) {
        for(CustomClass node : wheres)
            if(checkNode.isChild(node)) return true;
        return false;
    }

    public boolean has(RemoteClass checkNode) {
        return checkNode instanceof CustomClass && has((CustomClass)checkNode);
    }

    protected UpClassSet createThis(CustomClass[] iWheres) {
        return new UpClassSet(iWheres);
    }

    protected CustomClass[] newArray(int size) {
        return new CustomClass[size];
    }

    protected boolean containsAll(CustomClass who, CustomClass what) {
        return what.isChild(who);
    }

    protected CustomClass[] intersect(CustomClass where1, CustomClass where2) {
        CustomClassSet common = where1.commonChilds(where2);
        CustomClass[] result = new CustomClass[common.size];
        for(int i=0;i<common.size;i++)
            result[i] = common.get(i);
        return result;
    }

    public boolean inSet(UpClassSet up,ConcreteCustomClassSet set) { // проверяет находится ли в up,set - обратная containsAll
        for(CustomClass node : wheres)
            if(!node.upInSet(up,set)) return false;
        return true;
    }

    public AndClassSet and(AndClassSet node) {
        if(node instanceof ConcreteClass) {
            if(has((ConcreteClass)node))
                return node;
            else
                return UpClassSet.FALSE;
        } else
            return intersect((UpClassSet)node);
    }

    public OrClassSet getOr() {
        return new OrObjectClassSet(this);
    }

    public boolean isEmpty() {
        return wheres.length==0;
    }

    public boolean containsAll(AndClassSet node) {
        if(node instanceof ConcreteClass)
            return has((ConcreteClass)node);
        else
            return ((UpClassSet)node).inSet(this,new ConcreteCustomClassSet());
    }

    public ConcreteCustomClass getSingleClass() {
        return AbstractCustomClass.getSingleClass(wheres);
    }

    private String getChildString(String source) {
        String childString = "";
        Set<ConcreteCustomClass> children = new HashSet<ConcreteCustomClass>();
        for(CustomClass node : wheres)
            node.fillConcreteChilds(children);
        for(CustomClass child : children)
            childString = (childString.length()==0?"":childString+",") + child.ID;
        if(children.size()>0)
            childString = source + " IN (" + childString + ")";
        return childString;
    }

    public String getWhereString(String source) {
        return getChildString(source);
    }

    public String getNotWhereString(String source) {
        return "(" + source + " IS NULL OR NOT " + getChildString(source) + ")";
    }

    public Type getType() {
        return ObjectType.instance;
    }

    // чисто для getCommonParent
    public CustomClass[] getCommonClasses() {
        return wheres;
    }

    public UpClassSet add(UpClassSet set) {
        return super.add(set);
    }
    public UpClassSet intersect(UpClassSet set) {
        return super.intersect(set);
    }

}
