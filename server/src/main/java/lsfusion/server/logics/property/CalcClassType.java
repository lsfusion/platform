package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.property.infer.ExClassSet;

public class CalcClassType extends CalcType implements AlgType {

    public final static CalcInfoType PREVBASE = new CalcInfoType("PREVBASE"); // определение классов, prev'ы имеют base классы (что правильно, но не удобно)
    public final static CalcClassType PREVSAME = new CalcClassType("PREVSAME"); // определение классов, где prev'ы имеют те же классы (а не OBJECT)
    public final static CalcClassType PREVSAME_KEEPIS = new CalcClassType("PREVSAME_KEEPIS"); // тоже самое что и сверху, но с IS'ами нужно для одной эвристики

    public CalcClassType(String caption) {
        super(caption);
    }

    public <P extends PropertyInterface> ClassWhere<Object> getClassValueWhere(CalcProperty<P> property) {
        return property.calcClassValueWhere(this);
    }

    public <P extends PropertyInterface> ImMap<P, ValueClass> getInterfaceClasses(CalcProperty<P> property, ExClassSet valueClasses) {
        return property.calcInterfaceClasses(this);
    }

    public <P extends PropertyInterface> ValueClass getValueClass(CalcProperty<P> property) {
        return property.calcValueClass(this);
    }

    public <P extends PropertyInterface> boolean isInInterface(CalcProperty<P> property, ImMap<P, ? extends AndClassSet> interfaceClasses, boolean isAny) {
        return property.calcIsInInterface(interfaceClasses, isAny, this);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> void checkExclusiveness(CalcProperty<T> property, String caption, CalcProperty<P> intersect, String intersectCaption, ImRevMap<P, T> map) {
        property.calcCheckExclusiveness(caption, intersect, intersectCaption, map, this);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> void checkContainsAll(CalcProperty<T> property, CalcProperty<P> intersect, String caption, ImRevMap<P, T> map, CalcPropertyInterfaceImplement<T> value) {
        property.calcCheckContainsAll(intersect, map, this, value);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> void checkAllImplementations(CalcProperty<T> property, ImList<CalcProperty<P>> intersects, ImList<ImRevMap<P, T>> maps) {
        property.calcCheckAllImplementations(intersects, maps, this);
    }

    public boolean replaceIs() {
        return this == PREVSAME;
    }
}
