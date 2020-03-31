package lsfusion.server.logics.action.flow;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.ScriptParsingException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.UnionProperty;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public abstract class ListCaseAction extends KeepContextAction {

    private final PropertyMapImplement<UnionProperty.Interface, PropertyInterface> abstractWhere;
    protected boolean isExclusive;
    
    public enum AbstractType { CASE, MULTI, LIST }

    protected boolean checkExclusiveImplementations;
    protected boolean isLast;
    protected final AbstractType type;

    public boolean isAbstract() {
        return abstractWhere != null;
    }

    public AbstractType getAbstractType() {
        return type;
    }


    protected void addWhereOperand(ActionMapImplement<?, PropertyInterface> action, List<ResolveClassSet> signature, Version version) {
        ((CaseUnionProperty) abstractWhere.property).addOperand(action.mapWhereProperty().map(abstractWhere.mapping.reverse()), signature, version);
    }

    protected void addWhereCase(PropertyInterfaceImplement<PropertyInterface> where, ActionMapImplement<?, PropertyInterface> action, Version version) {
        ImRevMap<PropertyInterface, UnionProperty.Interface> abstractMap = abstractWhere.mapping.reverse();
        ((CaseUnionProperty) abstractWhere.property).addCase(where.map(abstractMap), action.mapWhereProperty().map(abstractMap), version);
    }

    // immutable реализация
    protected <I extends PropertyInterface> ListCaseAction(LocalizedString caption, boolean isExclusive, ImOrderSet<I> innerInterfaces) {
        super(caption, innerInterfaces.size());

        this.abstractWhere = null;
        this.type = null;
        this.isExclusive = isExclusive;
    }

    // mutable реализация
    public <I extends PropertyInterface> ListCaseAction(LocalizedString caption, boolean checkExclusiveImplementations, boolean checkAllImplementations, boolean isLast, AbstractType type, ImOrderSet<I> innerInterfaces, ImMap<I, ValueClass> mapClasses)  {
        super(caption, innerInterfaces.size());

        this.checkExclusiveImplementations = checkExclusiveImplementations;
        this.isLast = isLast;
        this.type = type;

        CaseUnionProperty.Type caseType = null;
        switch (type) {
            case CASE: caseType = CaseUnionProperty.Type.CASE; break;
            case MULTI: caseType = CaseUnionProperty.Type.MULTI; break;
            case LIST: caseType = CaseUnionProperty.Type.VALUE; break;
        }
        abstractWhere = PropertyFact.createUnion(checkExclusiveImplementations, checkAllImplementations, isLast, caseType, interfaces, LogicalClass.instance, getMapInterfaces(innerInterfaces).join(mapClasses));
    }

    protected abstract PropertyMapImplement<?, PropertyInterface> calcCaseWhereProperty();

    @IdentityInstanceLazy
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        if(isAbstract())
            return abstractWhere;

        return calcCaseWhereProperty();
    }

    protected void finalizeAbstractInit() {
        CaseUnionProperty caseProp = (CaseUnionProperty) abstractWhere.property;
        try {
            caseProp.finalizeInit();
        } catch (CaseUnionProperty.NotFullyImplementedException e) {
            throw new RuntimeException("Action is not fully implemented: " + this +  "\n\tCalculated: " + e.fullClassValueWhere + "\n\tSpecified: " + e.classValueWhere);
        } catch (ScriptParsingException e) {
            throw new ScriptParsingException("error finalizing abstract " + this + ":\n" + e.getMessage());
        }
    }
    
    @Override
    public void finalizeInit() {
        super.finalizeInit();

        if (isAbstract()) {
            finalizeAbstractInit();
        }
    }

    public void markRecursions(Set<Action> marks) {
        assert isAbstract();
        markRecursions(SetFact.EMPTY(), marks);
    }

    @Override
    protected void markRecursions(ImSet<ListCaseAction> recursiveActions, Set<Action> marks) {
        super.markRecursions(recursiveActions.addExcl(this), marks); // // пока исходим из того что рекурсивными могут быть только abstract'ы
    }

    protected abstract ImList<ActionMapImplement<?, PropertyInterface>> getListActions();

    public ImSet<Action> getDependActions() {
        return getListActions().mapListValues((Function<ActionMapImplement<?, PropertyInterface>, Action>) value -> value.action).toOrderSet().getSet();
    }
}
