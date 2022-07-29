package lsfusion.server.physics.dev.integration.external.to.mail;

import static lsfusion.base.BaseUtils.trimToEmpty;

public enum AccountType {
    POP3, IMAP, IMAPS;

    public static AccountType get(String name) {
        name = trimToEmpty(name);
        switch (name) {
            case "IMAP":
                return AccountType.IMAP;
            case "IMAPS":
                return AccountType.IMAPS;
            case "POP3":
            default:
                return AccountType.POP3;
        }
    }

    public String getProtocol() {
        switch (this) {
            case IMAP:
                return "imap";
            case IMAPS:
                return "imaps";
            case POP3:
            default:
                return "pop3";
        }
    }
}
