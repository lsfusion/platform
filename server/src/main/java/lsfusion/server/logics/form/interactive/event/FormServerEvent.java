package lsfusion.server.logics.form.interactive.event;

import lsfusion.interop.form.event.FormEvent;
import lsfusion.interop.form.event.FormEventClose;
import lsfusion.interop.form.event.FormScheduler;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.FormEventType;
import lsfusion.server.logics.form.interactive.MappingInterface;

public abstract class FormServerEvent<This extends FormServerEvent<This>> implements MappingInterface<This> {

    public static FormServerEvent getEventObject(FormEvent formEvent) {
        if (formEvent instanceof FormEventClose) {
            if (((FormEventClose) formEvent).ok)
                return FormEventType.QUERYOK;
            return FormEventType.QUERYCLOSE;
        }
        if(formEvent instanceof FormScheduler)
            return new FormServerScheduler(((FormScheduler) formEvent).period, ((FormScheduler) formEvent).fixed);
        throw new UnsupportedOperationException();
    }

    public static FormEvent getEventObject(FormServerEvent serverEvent) {
        if (serverEvent == FormEventType.QUERYOK)
            return FormEventClose.OK;
        if (serverEvent == FormEventType.QUERYCLOSE)
            return FormEventClose.CLOSE;
        if  (serverEvent instanceof FormServerScheduler)
            return new FormScheduler(((FormServerScheduler) serverEvent).period, ((FormServerScheduler) serverEvent).fixed);
        return null;
    }
}
