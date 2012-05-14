package platform.server.logics.property;

import platform.server.session.*;

public class OverrideModifier extends MutableModifier {

    private MutableModifier[] modifiers;

    protected void lateInit(MutableModifier... modifiers) {
        this.modifiers = modifiers;

        for(MutableModifier modifier : modifiers)
            modifier.registerView(this);
    }
    public OverrideModifier() {
    }

    public OverrideModifier(MutableModifier... modifiers) {
        lateInit(modifiers);
    }

    public <P extends PropertyInterface> ModifyChange<P> getModifyChange(CalcProperty<P> property) {
        ModifyChange<P> result = null;
        for (MutableModifier modifier : modifiers)
            result = ModifyChange.addNull(result, modifier.getPropertyChanges().getModify(property));
        return result;
    }

    public PropertyChanges calculatePropertyChanges() {
        PropertyChanges result = PropertyChanges.EMPTY;
        for (MutableModifier modifier : modifiers)
            result = result.add(modifier.calculatePropertyChanges());
        return result;
    }
}
