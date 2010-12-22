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
            int compare1 = getCompareInt(o1); int compare2 = getCompareInt(o2);
            return compare1>compare2?1:(compare1<compare2?-1:0);
        }
    };
}
