package equ.api;

public class ScalesInfo extends MachineryInfo {
    public String directory;
    public String pieceCodeGroupScales;
    public String weightCodeGroupScales;
    
    public ScalesInfo(Integer number, String nameModel, String handlerModel, String port, String directory,
                      String pieceCodeGroupScales, String weightCodeGroupScales) {
        this.number = number;
        this.nameModel = nameModel;
        this.handlerModel = handlerModel;
        this.port = port;
        this.directory = directory;
        this.pieceCodeGroupScales = pieceCodeGroupScales;
        this.weightCodeGroupScales = weightCodeGroupScales;
    }
}
