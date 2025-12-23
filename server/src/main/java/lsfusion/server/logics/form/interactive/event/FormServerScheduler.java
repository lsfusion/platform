package lsfusion.server.logics.form.interactive.event;

import lsfusion.interop.form.event.FormScheduler;
import lsfusion.server.logics.form.ObjectMapping;

import java.util.Objects;

public class FormServerScheduler extends FormServerEvent<FormServerScheduler> {

    public int period;
    public boolean fixed;

    public FormServerScheduler(int period, boolean fixed) {
        this.period = period;
        this.fixed = fixed;
    }

    @Override
    public FormServerScheduler get(ObjectMapping mapping) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormScheduler that = (FormScheduler) o;
        return period == that.period && fixed == that.fixed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(period, fixed);
    }
}
