package lsfusion.server.classes.sets;

import lsfusion.base.ExtraSetWhere;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.Settings;
import lsfusion.server.classes.*;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.ClassField;

import java.util.Arrays;

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

    public void fillNextConcreteChilds(MSet<ConcreteCustomClass> mSet) {
        for(CustomClass where : wheres)
            where.fillNextConcreteChilds(mSet);
    }

    public UpClassSet or(UpClassSet set) {
        return add(set);
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
            return or((UpClassSet)node);
        return getOr().or((OrObjectClassSet)node);
    }

    public OrObjectClassSet getOr() {
        return new OrObjectClassSet(this);
    }

    public boolean isEmpty() {
        return wheres.length==0;
    }

    public boolean containsAll(AndClassSet node, boolean implicitCast) {
        if(node instanceof ConcreteClass)
            return has((ConcreteClass)node);
        if(node instanceof UpClassSet)
            return ((UpClassSet)node).inSet(this, SetFact.<ConcreteCustomClass>EMPTY());
        return getOr().containsAll((OrClassSet)node, implicitCast);
    }

    public ConcreteCustomClass getSingleClass() {
        return AbstractCustomClass.getSingleClass(wheres);
    }

    public ImSet<ConcreteCustomClass> getSetConcreteChildren() {
        MSet<ConcreteCustomClass> children = SetFact.mSet();
        for(CustomClass node : wheres)
            node.fillConcreteChilds(children);
        return children.immutable();
    }

    public int getCount() {
        return OrObjectClassSet.getCount(this);
    }

    public int getClassCount() {
        return OrObjectClassSet.getClassCount(this);
    }

    public String getWhereString(String source) {
        return OrObjectClassSet.getWhereString(this, source);
    }

    public String getNotWhereString(String source) {
        return OrObjectClassSet.getNotWhereString(this, source);
    }

    public Type getType() {
        return ObjectType.instance;
    }

    public Stat getTypeStat(boolean forJoin) {
        return wheres[0].getTypeStat(forJoin);
    }

    // чисто для getCommonParent
    public CustomClass[] getCommonClasses() {
        return wheres;
    }

    protected CustomClass add(CustomClass addWhere, CustomClass[] wheres, int numWheres, CustomClass[] proceeded, int numProceeded) {
        if(!Settings.get().isMergeUpClassSets())
            return null;

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
                result = result.or(new UpClassSet(intersect(andOp,and)));
        return result;
    }

    public AndClassSet[] getAnd() {
        return new AndClassSet[]{this};
    }

    public ImRevMap<ClassField, ObjectValueClassSet> getTables() {
        MMap<ClassField, ObjectValueClassSet> mMap = MapFact.mMap(OrObjectClassSet.<ClassField>objectValueSetAdd());
        for(CustomClass customClass : wheres)
            mMap.addAll(customClass.getUpTables());
        return mMap.immutable().toRevExclMap();
    }

    public ValueClassSet getValueClassSet() {
        return this;
    }

    @Override
    public String toString() {
        return "up{" + Arrays.toString(wheres) + "}";
    }
}
