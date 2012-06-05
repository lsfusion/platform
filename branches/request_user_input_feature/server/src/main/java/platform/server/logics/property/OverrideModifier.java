package platform.server.logics.property;

import platform.server.session.*;

public class OverrideModifier extends MutableModifier {

    private Modifier[] modifiers;

    protected void lateInit(Modifier... modifiers) {
        this.modifiers = modifiers;

        for(Modifier modifier : modifiers) {
            if(modifier instanceof MutableModifier)
                ((MutableModifier)modifier).registerView(this);
            else
                propertyChanges = propertyChanges.add(modifier.getPropertyChanges());
        }
    }
    public OverrideModifier() {
    }

    public OverrideModifier(Modifier... modifiers) {
        lateInit(modifiers);
    }

    public <P extends PropertyInterface> ModifyChange<P> getModifyChange(CalcProperty<P> property) {
        ModifyChange<P> result = null;
        for (Modifier modifier : modifiers)
            result = ModifyChange.addNull(result, modifier.getPropertyChanges().getModify(property));
        return result;
    }

    public PropertyChanges calculatePropertyChanges() {
        PropertyChanges result = PropertyChanges.EMPTY;
        for (Modifier modifier : modifiers)
            result = result.add(modifier instanceof MutableModifier?
                    ((MutableModifier)modifier).calculatePropertyChanges():modifier.getPropertyChanges());
        return result;
    }
}
