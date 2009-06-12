package platform.server.data.classes.where;

import platform.base.QuickSet;
import platform.server.data.classes.ConcreteCustomClass;

public class ConcreteCustomClassSet extends QuickSet<ConcreteCustomClass,ConcreteCustomClassSet> {

    protected ConcreteCustomClass[] newArray(int size) {
        return new ConcreteCustomClass[size];
    }

    protected ConcreteCustomClassSet getThis() {
        return this;
    }

    public ConcreteCustomClassSet() {
    }

    public ConcreteCustomClassSet(ConcreteCustomClass customClass) {
        add(customClass);
    }

    // добавляет отфильтровывая up'ы
    public void addAll(ConcreteCustomClassSet set,UpClassSet up) {
        for(int i=0;i<set.size;i++) {
            ConcreteCustomClass nodeSet = set.table[set.indexes[i]];
            if(!up.has(nodeSet))
                add(nodeSet,set.htable[set.indexes[i]]);
        }
    }

    public boolean inSet(UpClassSet up,ConcreteCustomClassSet set) {
        for(int i=0;i<size;i++)
            if(!up.has(table[indexes[i]]) && !set.contains(table[indexes[i]])) return false;
        return true;
    }
}
