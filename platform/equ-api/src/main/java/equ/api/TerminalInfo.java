package equ.api;

public class TerminalInfo extends MachineryInfo {
    public String directory;
    public TerminalInfo(String directory, Integer number, String nameModel, String handlerModel, String port) {
        this.directory = directory;
        this.number = number;
        this.nameModel = nameModel;
        this.handlerModel = handlerModel;
        this.port = port;
    }
}
