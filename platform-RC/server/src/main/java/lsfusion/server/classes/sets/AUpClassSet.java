package lsfusion.server.classes.sets;

import lsfusion.base.ExtraMultiIntersectSetWhere;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;

import java.util.Arrays;

public abstract class AUpClassSet<This extends AUpClassSet<This>> extends ExtraMultiIntersectSetWhere<CustomClass, This> {

    protected AUpClassSet() {
    }

    protected AUpClassSet(CustomClass[] wheres) {
        super(wheres);
    }

    protected AUpClassSet(CustomClass where) {
        super(where);
    }

    protected CustomClass[] intersect(CustomClass where1, CustomClass where2) {
        ImSet<CustomClass> common = where1.commonChilds(where2);
        int size = common.size();
        CustomClass[] result = new CustomClass[size];
        for(int i=0;i<size;i++)
            result[i] = common.get(i);
        return result;
    }

    public boolean has(CustomClass checkNode) {
        for(CustomClass node : wheres)
            if(containsAll(node, checkNode)) return true;
        return false;
    }

    public This and(This set) {
        return intersect(set);
    }

    public This or(This set) {
        return add(set);
    }

    protected CustomClass[] newArray(int size) {
        return new CustomClass[size];
    }

    protected boolean containsAll(CustomClass who, CustomClass what) {
        return what.isChild(who);
    }

    public boolean isEmpty() {
        return wheres.length==0;
    }

    public Type getType() {
        return ObjectType.instance;
    }

    @Override
    public String toString() {
        return "up{" + Arrays.toString(wheres) + "}";
    }
}
