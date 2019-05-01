package lsfusion.gwt.client.base.exception;

import com.google.gwt.core.shared.SerializableThrowable;

import java.io.Serializable;

public class StackedException extends RuntimeException implements Serializable {

    public SerializableThrowable thisStack;
    public SerializableThrowable[] stacks;

    public StackedException() {
    }

    public StackedException(String message, SerializableThrowable thisStack, SerializableThrowable[] stacks) {
        super(message);
        this.thisStack = thisStack;
        this.stacks = stacks;
    }
}
