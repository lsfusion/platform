package lsfusion.server.logics.classes.data;

import lsfusion.base.ExceptionUtils;

public class ParseException extends Exception {

    public ParseException(String message) {
        super(message);
    }

    public static ParseException propagateWithMessage(String message, Throwable throwable) throws ParseException {
        ParseException propagatedMessage = new ParseException(ExceptionUtils.copyMessage(throwable) + ' ' + message);
        ExceptionUtils.copyStackTraces(throwable, propagatedMessage);
        throw propagatedMessage;
    }
}
