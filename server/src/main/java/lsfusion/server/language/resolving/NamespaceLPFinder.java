package lsfusion.server.language.resolving;

import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LP;

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
            List<ResolveClassSet> iParams = result.get(i).module.getParamClasses(iProp);
            boolean foundMoreSpecialized = false;
            for (int j = 0; j < cnt; j++) {
                L jProp = result.get(j).value;
                if (i != j && SignatureMatcher.isCompatible(iParams, result.get(j).module.getParamClasses(jProp), false, true) && 
                              !SignatureMatcher.isCompatible(result.get(j).module.getParamClasses(jProp), iParams, false, true)) {
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
