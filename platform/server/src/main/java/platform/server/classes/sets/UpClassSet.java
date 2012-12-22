package platform.server.classes.sets;

import platform.base.ExtraSetWhere;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.classes.*;
import platform.server.data.expr.query.Stat;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

// выше вершин
public class UpClassSet extends ExtraSetWhere<CustomClass,UpClassSet> implements ObjectValueClassSet {

    private UpClassSet(CustomClass[] classes) {
        super(classes);
    }

    public static final UpClassSet FALSE = new UpClassSet(new CustomClass[0]);

    public UpClassSet(CustomClass customClass) {
        this(new CustomClass[]{customClass});
    }

    public BaseClass getBaseClass() {
        return wheres[0].getBaseClass();
    }

    public AndClassSet getKeepClass() {
        return getBaseClass().getUpSet();
    }

    public boolean has(CustomClass checkNode) {
        for(CustomClass node : wheres)
            if(checkNode.isChild(node)) return true;
        return false;
    }

    public boolean has(RemoteClass checkNode) {
        return checkNode instanceof CustomClass && has((CustomClass)checkNode);
    }

    protected UpClassSet createThis(CustomClass[] wheres) {
        return new UpClassSet(wheres);
    }

    protected CustomClass[] newArray(int size) {
        return new CustomClass[size];
    }

    protected boolean containsAll(CustomClass who, CustomClass what) {
        return what.isChild(who);
    }

    protected CustomClass[] intersect(CustomClass where1, CustomClass where2) {
        ImSet<CustomClass> common = where1.commonChilds(where2);
        int size = common.size();
        CustomClass[] result = new CustomClass[size];
        for(int i=0;i<size;i++)
            result[i] = common.get(i);
        return result;
    }

    public boolean inSet(UpClassSet up,ImSet<ConcreteCustomClass> set) { // проверяет находится ли в up,set - обратная containsAll
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
        }
        if(node instanceof UpClassSet)
            return intersect((UpClassSet)node);
        return getOr().and((OrObjectClassSet)node);
    }

    public AndClassSet or(AndClassSet node) {
        if(node instanceof ConcreteClass)
            if(has((ConcreteClass)node))
                return this;
            else
                return OrObjectClassSet.or(this,node);
        if(node instanceof UpClassSet)
            return add((UpClassSet)node);
        return getOr().or((OrObjectClassSet)node);
    }

    public OrObjectClassSet getOr() {
        return new OrObjectClassSet(this);
    }

    public boolean isEmpty() {
        return wheres.length==0;
    }

    public boolean containsAll(AndClassSet node) {
        if(node instanceof ConcreteClass)
            return has((ConcreteClass)node);
        if(node instanceof UpClassSet)
            return ((UpClassSet)node).inSet(this, SetFact.<ConcreteCustomClass>EMPTY());
        return getOr().containsAll((OrClassSet)node);
    }

    public ConcreteCustomClass getSingleClass() {
        return AbstractCustomClass.getSingleClass(wheres);
    }

    private ImSet<ConcreteCustomClass> getConcreteChildren() {
        MSet<ConcreteCustomClass> children = SetFact.mSet();
        for(CustomClass node : wheres)
            node.fillConcreteChilds(children);
        return children.immutable();
    }

    private String getChildString(String source) {

        ImSet<ConcreteCustomClass> children = getConcreteChildren();
        if(children.size()==0) return Where.FALSE_STRING;
        return source + " IN (" + children.toString(new GetValue<String, ConcreteCustomClass>() {
            public String getMapValue(ConcreteCustomClass value) {
                return value.ID.toString();
            }}, ",") + ")";
    }

    public int getCount() {
        int stat = 0;
        for(ConcreteCustomClass child : getConcreteChildren())
            stat += child.getCount();
        return stat;
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

    public Stat getTypeStat() {
        return wheres[0].getTypeStat();
    }

    // чисто для getCommonParent
    public CustomClass[] getCommonClasses() {
        return wheres;
    }

    protected CustomClass add(CustomClass addWhere, CustomClass[] wheres, int numWheres, CustomClass[] proceeded, int numProceeded) {
        boolean empty = true;
        for (int i = 0; i < numWheres; i++)
            if (wheres[i] != null) {
                empty = false;
                break;
            }
        if (empty) {
            for (int i = 0; i < numProceeded; i++)
                if (proceeded[i] != null) {
                    empty = false;
                    break;
                }
            if (empty) // если в wheres и proceeded ничего не осталось, то более абстрактные классы не берем
                return null;
        }
        for(CustomClass parent : addWhere.parents)
            if(parent.upInSet(wheres, numWheres, proceeded, numProceeded, addWhere)) // если покрывает все where возвращаем parent
                return parent;
        return null;
    }

    protected UpClassSet intersect(UpClassSet where) {
        if(isTrue() || where.isFalse()) return where;
        if(isFalse() || where.isTrue()) return this;

        UpClassSet result = FALSE;
        for(CustomClass andOp : where.wheres)
            for(CustomClass and : wheres)
                result = result.add(new UpClassSet(intersect(andOp,and)));
        return result;
    }

    public AndClassSet[] getAnd() {
        return new AndClassSet[]{this};
    }
}
