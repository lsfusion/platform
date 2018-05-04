package lsfusion.server.logics.resolving;

import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.form.window.AbstractWindow;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.scripted.MetaCodeFragment;
import lsfusion.server.logics.table.ImplementTable;

import java.util.List;

public class ResolveManager {
    public LogicsModule LM;
    
    private ElementResolver<LP<?, ?>, List<ResolveClassSet>> directLPResolver;
    private ElementResolver<LP<?, ?>, List<ResolveClassSet>> abstractLPResolver;
    private ElementResolver<LP<?, ?>, List<ResolveClassSet>> abstractNotEqualLPResolver;
    private ElementResolver<LP<?, ?>, List<ResolveClassSet>> indirectLPResolver;
    private ElementResolver<AbstractGroup, ?> groupResolver;
    private ElementResolver<NavigatorElement, ?> navigatorResolver;
    private ElementResolver<FormEntity, ?> formResolver;
    private ElementResolver<AbstractWindow, ?> windowResolver;
    private ElementResolver<ImplementTable, ?> tableResolver;
    private ElementResolver<CustomClass, ?> classResolver;
    private ElementResolver<MetaCodeFragment, Integer> metaCodeFragmentResolver;


    public ResolveManager(LogicsModule LM) {
        this.LM = LM;
        initializeResolvers();
    }

    private void initializeResolvers() {
        directLPResolver = new LPResolver(LM, new ModuleLPFinder(), true, false);
        abstractLPResolver = new LPResolver(LM, new ModuleAbstractLPFinder(), true, false);
        abstractNotEqualLPResolver = new LPResolver(LM, new ModuleAbstractLPFinder(), true, true);
        indirectLPResolver = new LPResolver(LM, new ModuleSoftLPFinder(), false, false);
        groupResolver = new ElementResolver<>(LM, new ModuleGroupFinder());
        navigatorResolver = new ElementResolver<>(LM, new ModuleNavigatorElementFinder());
        formResolver = new ElementResolver<>(LM, new ModuleFormFinder());
        windowResolver = new ElementResolver<>(LM, new ModuleWindowFinder());
        tableResolver = new ElementResolver<>(LM, new ModuleTableFinder());
        classResolver = new ElementResolver<>(LM, new ModuleClassFinder());
        metaCodeFragmentResolver = new ElementResolver<>(LM, new ModuleMetaCodeFragmentFinder());
    }
    
    public LP<?, ?> findProperty(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        LP<?, ?> property = directLPResolver.resolve(compoundName, params);
        if (property == null) {
            property = indirectLPResolver.resolve(compoundName, params);
        }
        return property;
    }
    
    public LP<?, ?> findAbstractProperty(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        return getAbstractLPResolver(prioritizeNotEquals).resolve(compoundName, params);    
    } 
    
    public ValueClass findClass(String compoundName) throws ResolvingErrors.ResolvingError {
        return classResolver.resolve(compoundName);
    }

    public MetaCodeFragment findMetaCodeFragment(String compoundName, int paramCnt) throws ResolvingErrors.ResolvingError {
        return metaCodeFragmentResolver.resolve(compoundName, paramCnt);
    }

    public AbstractGroup findGroup(String compoundName) throws ResolvingErrors.ResolvingError {
        return groupResolver.resolve(compoundName);
    }    
    
    public AbstractWindow findWindow(String compoundName) throws ResolvingErrors.ResolvingError {
        return windowResolver.resolve(compoundName);
    }

    public NavigatorElement findNavigatorElement(String compoundName) throws ResolvingErrors.ResolvingError {
        return navigatorResolver.resolve(compoundName);
    }
    
    public FormEntity findForm(String compoundName) throws ResolvingErrors.ResolvingError {
        return formResolver.resolve(compoundName);
    }
    
    public ImplementTable findTable(String compoundName) throws ResolvingErrors.ResolvingError {
        return tableResolver.resolve(compoundName);
    }

    private ElementResolver<LP<?, ?>, List<ResolveClassSet>> getAbstractLPResolver(boolean prioritizeNotEquals) {
        if (prioritizeNotEquals) {
            return abstractNotEqualLPResolver;
        } else {
            return abstractLPResolver;
        }
    }
}
