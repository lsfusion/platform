package platform.server.data.classes.where;

import platform.base.QuickSet;
import platform.server.data.classes.ConcreteCustomClass;

public class ConcreteCustomClassSet extends QuickSet<ConcreteCustomClass> {

    public ConcreteCustomClassSet() {
    }

    public ConcreteCustomClassSet(ConcreteCustomClass customClass) {
        add(customClass);
    }

    // добавляет отфильтровывая up'ы
    public void addAll(ConcreteCustomClassSet set,UpClassSet up) {
        for(int i=0;i<set.size;i++) {
            ConcreteCustomClass nodeSet = set.get(i);
            if(!up.has(nodeSet))
                add(nodeSet,set.htable[set.indexes[i]]);
        }
    }

    public boolean inSet(UpClassSet up,ConcreteCustomClassSet set) {
        for(int i=0;i<size;i++)
            if(!up.has(get(i)) && !set.contains(get(i))) return false;
        return true;
    }
}
