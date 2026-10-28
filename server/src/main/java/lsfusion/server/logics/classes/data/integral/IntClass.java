package lsfusion.server.logics.classes.data.integral;

import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;

public abstract class IntClass<T extends Number> extends IntegralClass<T> {

    public IntClass(LocalizedString caption) {
        super(caption);
    }

    @Override
    public BigDecimal getSafeCastBound(boolean min) {
        return new BigDecimal(getInfiniteValue(min) + ".5");
    }

    @Override
    public String getIntervalStep() {
        return "1";
    }
}
