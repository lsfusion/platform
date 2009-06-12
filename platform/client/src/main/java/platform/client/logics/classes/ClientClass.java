package platform.client.logics.classes;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.logics.ClientCellView;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.Format;
import java.rmi.RemoteException;

abstract public class ClientClass implements Serializable {

    public abstract boolean hasChilds();

    abstract public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException;

    protected ClientClass(DataInputStream inStream) throws IOException {
    }

    public static ClientClass deserialize(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();

        if(type==0) return new ClientObjectClass(inStream);
        if(type==1) return new ClientIntegerClass(inStream);
        if(type==2) return new ClientLongClass(inStream);
        if(type==3) return new ClientDoubleClass(inStream);
        if(type==4) return new ClientNumericClass(inStream);
        if(type==5) return new ClientBitClass(inStream);
        if(type==6) return new ClientDateClass(inStream);
        if(type==7) return new ClientStringClass(inStream);

        throw new IOException();
    }

    public abstract ClientType getType();
}
