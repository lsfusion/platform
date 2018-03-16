package lsfusion.server.logics.resolving;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LP;

import java.util.ArrayList;
import java.util.List;

/**
 * Поиск свойства/действия 
 * Из всех вариантов отбирает те, для которых не существует более подходящих вариантов
 */

public class NamespaceLPFinder<L extends LP<?, ?>> extends NamespaceElementFinder<L, List<ResolveClassSet>> {

    public NamespaceLPFinder(ModuleFinder<L, List<ResolveClassSet>> finder, List<LogicsModule> modules) {
        super(finder, modules);
    }

    @Override
    protected List<FoundItem<L>> finalizeResult(final List<FoundItem<L>> result) {
        return filterFoundProperties(result);
    }
    
    public static <L extends LP<?,?>> List<FoundItem<L>> filterFoundProperties(List<FoundItem<L>> result) {
        int cnt = result.size();
        List<FoundItem<L>> finalResult = new ArrayList<>();
        for (int i = 0; i < cnt; i++) {
            L iProp = result.get(i).value;
            List<ResolveClassSet> iParams = result.get(i).module.propClasses.get(iProp);
            boolean foundMoreSpecialized = false;
            for (int j = 0; j < cnt; j++) {
                L jProp = result.get(j).value;
                if (i != j && SignatureMatcher.isCompatible(iParams, result.get(j).module.propClasses.get(jProp), false, true) && 
                              !SignatureMatcher.isCompatible(result.get(j).module.propClasses.get(jProp), iParams, false, true)) {
                    foundMoreSpecialized = true;
                    break;
                }
            }
            if (!foundMoreSpecialized) {
                finalResult.add(result.get(i));
            }
        }
        return finalResult;        
    }
}
