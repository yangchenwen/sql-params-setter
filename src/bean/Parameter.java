package bean;

import static constants.Const.*;

/**
 * @author yangchenwen
 * @since 2020-06-24 11:27:48
 */
public class Parameter {

    private final String value;
    private final Type type;

    public Parameter(String value, Type type) {
        this.value = value;
        this.type = type;
    }

    public static Parameter of(String param) {
        if (!END_WITH_PAREN.matcher(param).find()) {
            return new Parameter(param.trim(), Type.STRING);
        }
        String value = param.substring(0, param.indexOf(L_BRACKET)).trim();
        Type type = Type.OTHER;
        try {
            type = Type.valueOf(param.substring(param.indexOf(L_BRACKET) + 1, param.indexOf(R_BRACKET)).toUpperCase());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return new Parameter(value, type);
    }

    public String getValue() {
        return type.decorate(value);
    }

    enum Type {

        STRING {
            @Override
            public String decorate(String value) {
                return String.format("'%s'", value);
            }
        },

        DATE {
            @Override
            public String decorate(String value) {
                return String.format("'%s'", value);
            }
        },

        TIMESTAMP {
            @Override
            public String decorate(String value) {
                return String.format("'%s'", value);
            }
        },

        TIME {
            @Override
            public String decorate(String value) {
                return String.format("'%s'", value);
            }
        },

        OTHER {
            @Override
            public String decorate(String value) {
                return value;
            }
        };

        abstract public String decorate(String value);
    }
}
