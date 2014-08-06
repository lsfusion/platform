package lsfusion.server.logics.property;

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

    public boolean replaceIs() {
        return this == PREVSAME;
    }
}
