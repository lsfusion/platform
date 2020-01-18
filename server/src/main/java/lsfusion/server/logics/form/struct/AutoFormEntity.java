package lsfusion.server.logics.form.struct;

import lsfusion.server.base.version.Version;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class AutoFormEntity extends FormEntity {

    public AutoFormEntity(LocalizedString caption, Version version) {
        super(null, null, caption, null, version);
    }
}
