package platform.client.logics;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Serializer {

    public static byte[] serializeClientGroupObjectValue(ClientGroupObjectValue clientGroupObjectValue) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            clientGroupObjectValue.serialize(new DataOutputStream(outStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outStream.toByteArray();
    }

    // -------------------------------------- Сериализация фильтров -------------------------------------------- //
    public static byte[] serializeClientFilter(ClientFilter filter) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            filter.serialize(new DataOutputStream(outStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outStream.toByteArray();
    }
}
