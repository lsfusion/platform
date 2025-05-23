package lsfusion.server.logics.form.struct.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.print.ReportFieldExtraType;
import lsfusion.interop.form.property.PropertyReadType;

public enum PropertyDrawExtraType {
    CAPTION {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.HEADER;
        }
        
        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.CAPTION;
        }
        
        @Override
        public String getText() {
            return "{logics.property.caption}";    
        }
    },
    FOOTER {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.FOOTER;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.FOOTER;
        }
        
        @Override
        public String getText() {
            return "{logics.property.footer}";
        }
    }, 
    SHOWIF {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.SHOWIF;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.SHOWIF;
        }
        
        @Override
        public String getText() {
            return "SHOWIF";
        }
    }, 
    READONLYIF {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.READONLY;
        }

        @Override
        public String getText() {
            return "{logics.property.readonly}";
        }
    },
    GRIDELEMENTCLASS {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.CELL_GRIDELEMENTCLASS;
        }

        @Override
        public String getText() {
            return "GRIDELEMENTCLASS";
        }
    },
    VALUEELEMENTCLASS {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.CELL_VALUEELEMENTCLASS;
        }

        @Override
        public String getText() {
            return "VALUEELEMENTCLASS";
        }
    }, 
    CAPTIONELEMENTCLASS {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.CAPTIONELEMENTCLASS;
        }

        @Override
        public String getText() {
            return "CAPTIONELEMENTCLASS";
        }
    },
    FONT {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.FONT;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.CELL_FONT;
        }

        @Override
        public String getText() {
            return "{logics.font}";
        }
    },
    BACKGROUND {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.BACKGROUND;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.CELL_BACKGROUND;
        }

        @Override
        public String getText() {
            return "{logics.background}";
        }
    },
    FOREGROUND {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.FOREGROUND;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.CELL_FOREGROUND;
        }

        @Override
        public String getText() {
            return "{logics.foreground}";
        }
    },
    IMAGE {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return ReportFieldExtraType.IMAGE;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.IMAGE;
        }

        @Override
        public String getText() {
            return "{logics.image}";
        }
    },
    COMMENT {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.COMMENT;
        }

        @Override
        public String getText() {
            return "COMMENT";
        }
    },
    COMMENTELEMENTCLASS {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.COMMENTELEMENTCLASS;
        }

        @Override
        public String getText() {
            return "COMMENTELEMENTCLASS";
        }
    },
    PLACEHOLDER {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.PLACEHOLDER;
        }

        @Override
        public String getText() {
            return "PLACEHOLDER";
        }
    },
    PATTERN {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.PATTERN;
        }

        @Override
        public String getText() {
            return "PATTERN";
        }
    },
    REGEXP {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.REGEXP;
        }

        @Override
        public String getText() {
            return "REGEXP";
        }
    },
    REGEXPMESSAGE {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.REGEXPMESSAGE;
        }

        @Override
        public String getText() {
            return "REGEXPMESSAGE";
        }
    },
    TOOLTIP {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.TOOLTIP;
        }

        @Override
        public String getText() {
            return "TOOLTIP";
        }
    },
    VALUETOOLTIP {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.VALUETOOLTIP;
        }

        @Override
        public String getText() {
            return "VALUETOOLTIP";
        }
    },
    PROPERTY_CUSTOM_OPTIONS {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.PROPERTY_CUSTOM_OPTIONS;
        }

        @Override
        public String getText() {
            return "PROPERTY_CUSTOM_OPTIONS";
        }
    },
    CHANGEKEY {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.CHANGEKEY;
        }

        @Override
        public String getText() {
            return "CHANGEKEY";
        }
    },
    CHANGEMOUSE {
        @Override
        public ReportFieldExtraType getReportExtraType() {
            return null;
        }

        @Override
        public byte getPropertyReadType() {
            return PropertyReadType.CHANGEMOUSE;
        }

        @Override
        public String getText() {
            return "CHANGEMOUSE";
        }
    }
    ;

    public static final ImSet<PropertyDrawExtraType> extras = SetFact.toSet(values());

    public ReportFieldExtraType getReportExtraType() {
        throw new UnsupportedOperationException();
    }

    public byte getPropertyReadType() {
        throw new UnsupportedOperationException();
    }
    
    public String getText() {
        return "";
    }
}
