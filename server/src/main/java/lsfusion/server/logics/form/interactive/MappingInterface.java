package lsfusion.server.logics.form.interactive;

import lsfusion.server.logics.form.ObjectMapping;

public interface MappingInterface<This extends MappingInterface<This>> {

    This get(ObjectMapping mapping);
}
