package platform.base;

import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;

// объект Immutable и не подвержен memory leak и соответственно содержит только ссылки на GlobalObject'ы
public interface GlobalObject {
    
    Map<GlobalObject, Integer> compareMap = new HashMap<GlobalObject, Integer>();
    Comparator<GlobalObject> comparator = new Comparator<GlobalObject>() {
        private int getCompareInt(GlobalObject concreteClass) {
            synchronized(compareMap) {
                Integer compareInt = compareMap.get(concreteClass);
                if(compareInt==null) {
                    compareInt = compareMap.size();
                    compareMap.put(concreteClass, compareInt);
                }
                return compareInt;
            }
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
                compare1 = getCompareInt(o1);
                compare2 = getCompareInt(o2);
            }

            return compare1>compare2?1:(compare1<compare2?-1:0);
        }
    };
}
