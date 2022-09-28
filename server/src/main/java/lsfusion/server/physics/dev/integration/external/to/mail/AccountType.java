package lsfusion.server.physics.dev.integration.external.to.mail;

import static lsfusion.base.BaseUtils.trimToEmpty;

public enum AccountType {
    POP3, POP3S, IMAP, IMAPS;

    public static AccountType get(String name) {
        name = trimToEmpty(name);
        switch (name) {
            case "POP3S":
                return AccountType.POP3S;
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
            case POP3S:
                return "pop3s";
            case IMAP:
                return "imap";
            case IMAPS:
                return "imaps";
            case POP3:
            default:
                return "pop3";
        }
    }

    public String getHost() {
        switch (this) {
            case POP3S:
                return "mail.pop3s.host";
            case IMAP:
            case IMAPS:
                return "mail.imap.host";
            case POP3:
            default:
                return "mail.pop3.host";
        }
    }

}
