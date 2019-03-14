package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.physics.dev.id.name.CompoundNameUtils;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.form.struct.group.AbstractGroup;
import lsfusion.server.language.MetaCodeFragment;
import lsfusion.server.physics.exec.table.ImplementTable;

import java.util.List;

public class ResolveManager {
    public LogicsModule LM;
    
    private ElementResolver<LCP<?>, List<ResolveClassSet>> directLCPResolver;
    private ElementResolver<LCP<?>, List<ResolveClassSet>> abstractLCPResolver;
    private ElementResolver<LCP<?>, List<ResolveClassSet>> abstractNotEqualLCPResolver;
    private ElementResolver<LCP<?>, List<ResolveClassSet>> indirectLCPResolver;
    private ElementResolver<LCP<?>, List<ResolveClassSet>> directLocalsResolver;
    private ElementResolver<LCP<?>, List<ResolveClassSet>> indirectLocalsResolver;

    private ElementResolver<LAP<?>, List<ResolveClassSet>> directLAPResolver;
    private ElementResolver<LAP<?>, List<ResolveClassSet>> abstractLAPResolver;
    private ElementResolver<LAP<?>, List<ResolveClassSet>> abstractNotEqualLAPResolver;
    private ElementResolver<LAP<?>, List<ResolveClassSet>> indirectLAPResolver;
    
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
        directLCPResolver = new LPResolver<>(LM, new ModuleLCPFinder(), true, false);
        abstractLCPResolver = new LPResolver<>(LM, new ModuleAbstractLCPFinder(), true, false);
        abstractNotEqualLCPResolver = new LPResolver<>(LM, new ModuleAbstractLCPFinder(), true, true);
        indirectLCPResolver = new LPResolver<>(LM, new ModuleIndirectLCPFinder(), false, false);
        directLocalsResolver = new LPResolver<>(LM, new ModuleDirectLocalsFinder(), true, false);
        indirectLocalsResolver = new LPResolver<>(LM, new ModuleIndirectLocalsFinder(), false, false);
        
        directLAPResolver = new LPResolver<>(LM, new ModuleLAPFinder(), true, false);
        abstractLAPResolver = new LPResolver<>(LM, new ModuleAbstractLAPFinder(), true, false);
        abstractNotEqualLAPResolver = new LPResolver<>(LM, new ModuleAbstractLAPFinder(), true, true);
        indirectLAPResolver = new LPResolver<>(LM, new ModuleIndirectLAPFinder(), false, false);
        
        groupResolver = new ElementResolver<>(LM, new ModuleGroupFinder());
        navigatorResolver = new ElementResolver<>(LM, new ModuleNavigatorElementFinder());
        formResolver = new ElementResolver<>(LM, new ModuleFormFinder());
        windowResolver = new ElementResolver<>(LM, new ModuleWindowFinder());
        tableResolver = new ElementResolver<>(LM, new ModuleTableFinder());
        classResolver = new ElementResolver<>(LM, new ModuleClassFinder());
        metaCodeFragmentResolver = new ElementResolver<>(LM, new ModuleMetaCodeFragmentFinder());
    }
    
    public LCP<?> findProperty(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        LCP<?> property = null;
        if (!CompoundNameUtils.hasNamespace(compoundName)) {
            property = findLocalProperty(compoundName, params);
        }
        if (property == null) {
            property = findGlobalProperty(compoundName, params);
        }
        return property;
    }
    
    private LCP<?> findLocalProperty(String name, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        LCP<?> property = directLocalsResolver.resolve(name, params);
        if (property == null) {
            property = indirectLocalsResolver.resolve(name, params);
        }
        return property;
    }

    private LCP<?> findGlobalProperty(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        LCP<?> property = directLCPResolver.resolve(compoundName, params);
        if (property == null) {
            property = indirectLCPResolver.resolve(compoundName, params);
        }
        return property;
    }
    
    public LCP<?> findAbstractProperty(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        return getAbstractLCPResolver(prioritizeNotEquals).resolve(compoundName, params);    
    } 

    public LAP<?> findAction(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        LAP<?> property = directLAPResolver.resolve(compoundName, params);
        if (property == null) {
            property = indirectLAPResolver.resolve(compoundName, params);
        }
        return property;
    }
    
    public LAP<?> findAbstractAction(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        return getAbstractLAPResolver(prioritizeNotEquals).resolve(compoundName, params);    
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

    private ElementResolver<LCP<?>, List<ResolveClassSet>> getAbstractLCPResolver(boolean prioritizeNotEquals) {
        if (prioritizeNotEquals) {
            return abstractNotEqualLCPResolver;
        } else {
            return abstractLCPResolver;
        }
    }

    private ElementResolver<LAP<?>, List<ResolveClassSet>> getAbstractLAPResolver(boolean prioritizeNotEquals) {
        if (prioritizeNotEquals) {
            return abstractNotEqualLAPResolver;
        } else {
            return abstractLAPResolver;
        }
    }
}
