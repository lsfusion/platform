package equ.api;

public class ScalesInfo extends MachineryInfo {
    public String directory;
    public String pieceItemCodeGroupScales;
    public String weightItemCodeGroupScales;
    
    public ScalesInfo(Integer number, String nameModel, String handlerModel, String port, String directory,
                      String pieceItemCodeGroupScales, String weightItemCodeGroupScales) {
        this.number = number;
        this.nameModel = nameModel;
        this.handlerModel = handlerModel;
        this.port = port;
        this.directory = directory;
        this.pieceItemCodeGroupScales = pieceItemCodeGroupScales;
        this.weightItemCodeGroupScales = weightItemCodeGroupScales;
    }
}
