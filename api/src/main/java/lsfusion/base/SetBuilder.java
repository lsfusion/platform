package lsfusion.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SetBuilder<T> {

    static <T> void recFillSubSetList(List<T> buildSet,int current,List<List<T>> result, ArrayList<T> currentSet) {
        if(current>=buildSet.size()) {
            result.add((List<T>)currentSet.clone());
            return;
        }

        recFillSubSetList(buildSet,current+1,result,currentSet);
        currentSet.add(buildSet.get(current));
        recFillSubSetList(buildSet,current+1,result,currentSet);
        currentSet.remove(buildSet.get(current));
    }

    // строит список подмн-в в лексикографическом порядке
    public static <T> List<List<T>> buildSubSetList(Collection<T> buildSet) {

        List<T> buildList;
        if(buildSet instanceof List)
            buildList = (List<T>)buildSet;
        else
            buildList = new ArrayList<T>(buildSet);

        List<List<T>> result = new ArrayList<List<T>>();
        recFillSubSetList(buildList,0,result,new ArrayList<T>());
        return result;
    }

    public static <T> void recBuildSetCombinations(int count, List<T> listElements, int current, ArrayList<T> currentList, Collection<List<T>> result) {

        if (currentList.size() == count) {
            result.add((List<T>)currentList.clone());
            return;
        }

        if (current >= listElements.size()) return;

        recBuildSetCombinations(count, listElements, current+1, currentList, result);
        currentList.add(listElements.get(current));
        recBuildSetCombinations(count, listElements, current+1, currentList, result);
        currentList.remove(listElements.get(current));
    }

    public static <T> Collection<List<T>> buildSetCombinations(int count, List<T> listElements) {

        Collection<List<T>> result = new ArrayList<List<T>>();
        recBuildSetCombinations(count, listElements, 0, new ArrayList<T>(), result);
        return result;
    }
}
