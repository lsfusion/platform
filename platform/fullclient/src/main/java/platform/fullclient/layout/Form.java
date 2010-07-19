package platform.fullclient.layout;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class Form {
    private int type;
    private String name;

    public Form(){}

    public Form(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }



    public void read( DataInputStream in ) throws IOException {
        name = in.readUTF();
        type = in.readInt();
        //for( PictureListener listener : listeners.toArray( new PictureListener[ listeners.size() ] ))
        //    listener.pictureChanged();
    }

     public void write( DataOutputStream out ) throws IOException{
         out.writeUTF(name);
         out.writeInt(type);
    }
}
