package platform.client.logics;

import platform.base.BaseUtils;
import platform.base.identity.IdentityObject;
import platform.client.ClientResourceBundle;
import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.base.context.ApplicationContext;
import platform.gwt.view.GObject;
import platform.interop.FormEventType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ClientObject extends IdentityObject implements Serializable, ClientIdentitySerializable {

    public String caption;
    public Set<FormEventType> addOnEvent = new HashSet<FormEventType>();

    // вручную заполняется
    public ClientGroupObject groupObject;

    public ClientClass baseClass;

    public ClientClassChooser classChooser;

    public ClientObject() {

    }

    public ClientObject(int ID, ApplicationContext context) {
        super(ID);
        classChooser = new ClientClassChooser(ID, context);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, classChooser);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        groupObject = pool.deserializeObject(inStream);

        caption = pool.readString(inStream);

        addOnEvent = pool.readObject(inStream);

        baseClass = ClientTypeSerializer.deserializeClientClass(inStream);

        classChooser = pool.deserializeObject(inStream);
    }

    public String getCaption(){
        return !BaseUtils.isRedundantString(caption)
                ? caption
                : !BaseUtils.isRedundantString(baseClass)
                ? baseClass.toString()
                : ClientResourceBundle.getString("logics.undefined.object");
    }

    @Override
    public String toString() {
        return !getCaption().equals(ClientResourceBundle.getString("logics.undefined.object"))
                ? getCaption() + " (" + getID() + ")"
                : getCaption();
    }

    private GObject gwtObject;
    public GObject getGwtObject() {
        if (gwtObject == null) {
            gwtObject = new GObject();
            gwtObject.ID = ID;
            gwtObject.sID = getSID();
            gwtObject.caption = getCaption();
        }

        return gwtObject;
    }
}
