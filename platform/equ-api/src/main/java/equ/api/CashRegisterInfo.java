package equ.api;

public class CashRegisterInfo extends MachineryInfo {
    public String directory;
    public String cashRegisterNumber;
    public Integer roundSales;

    public CashRegisterInfo(Integer number, String cashRegisterNumber, String nameModel, String handlerModel,
                            String port, String directory, Integer roundSales) {
        this.cashRegisterNumber = cashRegisterNumber;
        this.number = number;
        this.nameModel = nameModel;
        this.handlerModel = handlerModel;
        this.port = port;
        this.directory = directory;
        this.roundSales = roundSales;
    }
}
