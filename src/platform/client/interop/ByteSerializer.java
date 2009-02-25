package platform.client.interop;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ByteSerializer {
    
    public static byte[] serializeClientGroupObjectValue(ClientGroupObjectValue clientGroupObjectValue) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            clientGroupObjectValue.serialize(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    // -------------------------------------- Сериализация фильтров -------------------------------------------- //
    public static byte[] serializeClientFilter(ClientFilter filter) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            filter.serialize(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }
}
