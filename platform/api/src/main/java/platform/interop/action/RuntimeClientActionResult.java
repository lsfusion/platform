package platform.interop.action;

public class RuntimeClientActionResult extends ClientActionResult {

    public byte[] output;
    public byte[] input;

    public RuntimeClientActionResult(byte[] output, byte[] input) {
        this.output = output;
        this.input = input;
    }
}
