package lsfusion.server.form.navigator;

import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;

public class NavigatorForm extends NavigatorElement {
    private FormEntity form;
    
    public NavigatorForm(FormEntity form, String canonicalName, LocalizedString caption) {
        super(canonicalName, caption);
        
        this.form = form;
        setImage("/images/form.png", DefaultIcon.FORM);
    }

    @Override
    public boolean isLeafElement() {
        return true;
    }

    @Override
    public byte getTypeID() {
        return 0;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeUTF(form.modalityType.name());
        outStream.writeUTF(form.getCanonicalName());
        outStream.writeUTF(form.getSID());
    }

    public FormEntity getForm() {
        return form;
    }
    
}
