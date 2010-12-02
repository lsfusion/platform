package platform.server.classes;

import platform.server.classes.sets.AndClassSet;

import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;

public interface ConcreteClass extends RemoteClass, AndClassSet {

    // у всех одинаковая реализация - проблема множественного наследования
    boolean inSet(AndClassSet set);

    final static Map<ConcreteClass, Integer> compareMap = new HashMap<ConcreteClass, Integer>(); 
    final static Comparator<ConcreteClass> comparator = new Comparator<ConcreteClass>() {
        private int getCompareInt(ConcreteClass concreteClass) {
            synchronized(compareMap) {
                Integer compareInt = compareMap.get(concreteClass);
                if(compareInt==null) {
                    compareInt = compareMap.size();
                    compareMap.put(concreteClass, compareInt);
                }
                return compareInt;
            }
        }

        public int compare(ConcreteClass o1, ConcreteClass o2) {
            int compare1 = getCompareInt(o1); int compare2 = getCompareInt(o2);
            return compare1>compare2?1:(compare1<compare2?-1:0);
        }
    };
    
}
