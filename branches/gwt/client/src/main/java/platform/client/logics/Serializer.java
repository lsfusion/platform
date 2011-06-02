package platform.client.logics;

import platform.client.logics.filter.ClientPropertyFilter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Serializer {

    // -------------------------------------- Сериализация фильтров -------------------------------------------- //
    public static byte[] serializeClientFilter(ClientPropertyFilter filter) throws IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        filter.serialize(new DataOutputStream(outStream));
        return outStream.toByteArray();
    }
}
