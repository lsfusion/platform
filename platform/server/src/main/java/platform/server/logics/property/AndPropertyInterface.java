package platform.server.logics.property;

public class AndPropertyInterface extends FormulaPropertyInterface<AndPropertyInterface> {

    public boolean not;

    public AndPropertyInterface(int iID, boolean iNot) {
        super(iID);
        not = iNot;
    }
}
