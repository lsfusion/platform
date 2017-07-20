package lsfusion.interop.action;

import java.io.Serializable;

public class RuntimeClientActionResult implements Serializable {

    public byte[] output;
    public byte[] input;

    public RuntimeClientActionResult(byte[] output, byte[] input) {
        this.output = output;
        this.input = input;
    }
}
