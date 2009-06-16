package platform.server.logics.properties;

public class AndPropertyInterface extends FormulaPropertyInterface<AndPropertyInterface> {

    public boolean not;

    public AndPropertyInterface(int iID, boolean iNot) {
        super(iID);
        not = iNot;
    }
}
