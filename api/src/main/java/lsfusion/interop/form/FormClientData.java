package lsfusion.interop.form;

import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;

import java.io.Serializable;
import java.util.Set;

public class FormClientData implements Serializable {

    public String formSID;
    public String canonicalName;

    public FormUserPreferences userPreferences;
    public byte[] richDesign;
    public Set<Integer> inputGroupObjects;

    public byte[] firstChanges;

    public FormClientData(String formSID, String canonicalName, FormUserPreferences userPreferences, byte[] richDesign, Set<Integer> inputGroupObjects, byte[] firstChanges) {
        this.formSID = formSID;
        this.canonicalName = canonicalName;
        this.userPreferences = userPreferences;
        this.richDesign = richDesign;
        this.inputGroupObjects = inputGroupObjects;
        this.firstChanges = firstChanges;
    }
}
