package platform.server.classes.sets;

import platform.base.QuickSet;
import platform.server.classes.ConcreteCustomClass;

public class ConcreteCustomClassSet extends QuickSet<ConcreteCustomClass> {

    public ConcreteCustomClassSet() {
    }

    public ConcreteCustomClassSet(ConcreteCustomClass customClass) {
        add(customClass);
    }

    // добавляет отфильтровывая up'ы
    public void addAll(ConcreteCustomClassSet set, UpClassSet up, boolean has) {
        for(int i=0;i<set.size;i++) {
            ConcreteCustomClass nodeSet = set.get(i);
            if(up.has(nodeSet)==has)
                add(nodeSet,set.htable[set.indexes[i]]);
        }
    }

    public void addAll(ConcreteCustomClassSet set, ConcreteCustomClassSet and) {
        for(int i=0;i<set.size;i++) {
            ConcreteCustomClass nodeSet = set.get(i);
            if(and.contains(nodeSet))
                add(nodeSet,set.htable[set.indexes[i]]);
        }
    }

    public boolean inSet(UpClassSet up,ConcreteCustomClassSet set) {
        for(int i=0;i<size;i++)
            if(!up.has(get(i)) && !set.contains(get(i))) return false;
        return true;
    }

    public ConcreteCustomClassSet remove(UpClassSet up) {
        ConcreteCustomClassSet result = new ConcreteCustomClassSet();
        for(int i=0;i<size;i++) {
            ConcreteCustomClass nodeSet = get(i);
            if(!up.has(nodeSet))
                result.add(nodeSet,htable[indexes[i]]);
        }
        return result;
    }
}
