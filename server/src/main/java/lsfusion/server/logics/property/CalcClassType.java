package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.where.classes.ClassWhere;

public class CalcClassType extends CalcType implements AlgType {

    public final static CalcInfoType PREVBASE = new CalcInfoType(); // определение классов, prev'ы имеют base классы (что правильно, но не удобно) 
    public final static CalcClassType PREVSAME = new CalcClassType(); // определение классов, где prev'ы имеют те же классы (а не OBJECT)
    public final static CalcClassType PREVSAME_KEEPIS = new CalcClassType(); // тоже самое что и сверху, но с IS'ами нужно для одной эвристики 

    public CalcClassType() {
    }

    public <P extends PropertyInterface> ClassWhere<Object> getClassValueWhere(CalcProperty<P> property) {
        return property.calcClassValueWhere(this);
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
