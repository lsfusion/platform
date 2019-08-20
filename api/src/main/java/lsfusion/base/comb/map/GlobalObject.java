package lsfusion.base.comb.map;

import lsfusion.base.col.MapFact;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// объект Immutable и не подвержен memory leak и соответственно содержит только ссылки на GlobalObject'ы
public interface GlobalObject {
    
    ConcurrentHashMap<GlobalObject, Integer> uniqueCompareMap = MapFact.getGlobalConcurrentHashMap();
    AtomicInteger globalCounter = new AtomicInteger();
    Comparator<GlobalObject> comparator = new Comparator<GlobalObject>() {
        private int getUniqueCompareInt(GlobalObject globalObject) {
            Integer compareInt = uniqueCompareMap.get(globalObject);
            if(compareInt==null) {
                compareInt = globalCounter.incrementAndGet();
                uniqueCompareMap.put(globalObject, compareInt);
            }
            return compareInt;
        }

        public int compare(GlobalObject o1, GlobalObject o2) {
            int compare1, compare2;
            if(o1 instanceof GlobalInteger) { // отдельно отработаем GlobalInteger'ы
                if(o2 instanceof GlobalInteger) {
                    compare1 = ((GlobalInteger)o1).integer;
                    compare2 = ((GlobalInteger)o2).integer;
                } else
                    return 1;
            } else
            if(o2 instanceof GlobalInteger) {
                return -1;
            } else {
                compare1 = o1.hashCode();
                compare2 = o2.hashCode();
                if(compare1 == compare2) {
                    compare1 = getUniqueCompareInt(o1);
                    compare2 = getUniqueCompareInt(o2);
                }
            }
            return Integer.compare(compare1, compare2);
        }
    };
}
