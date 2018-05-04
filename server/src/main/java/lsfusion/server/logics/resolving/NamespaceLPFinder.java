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

public class NamespaceLPFinder extends NamespaceElementFinder<LP<?, ?>, List<ResolveClassSet>> {

    public NamespaceLPFinder(ModuleFinder<LP<?, ?>, List<ResolveClassSet>> finder, List<LogicsModule> modules) {
        super(finder, modules);
    }

    @Override
    protected List<FoundItem<LP<?, ?>>> finalizeResult(final List<FoundItem<LP<?, ?>>> result) {
        return filterFoundProperties(result);
    }
    
    public static List<FoundItem<LP<?, ?>>> filterFoundProperties(List<FoundItem<LP<?, ?>>> result) {
        int cnt = result.size();
        List<FoundItem<LP<?, ?>>> finalResult = new ArrayList<>();
        for (int i = 0; i < cnt; i++) {
            LP<?, ?> iProp = result.get(i).value;
            List<ResolveClassSet> iParams = result.get(i).module.propClasses.get(iProp);
            boolean foundMoreSpecialized = false;
            for (int j = 0; j < cnt; j++) {
                LP<?, ?> jProp = result.get(j).value;
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
