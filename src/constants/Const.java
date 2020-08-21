package constants;

import java.util.regex.Pattern;

/**
 * @author yangchenwen
 * @since 2020-06-09 15:27:41
 */
public interface Const {

    String PALACE_HOLDER = "?";
    String COMMA = ",";
    String SEMICOLON = ";";
    String L_BRACKET = "(";
    String R_BRACKET = ")";
    String SEPARATOR_PREPARING = "Preparing:";
    String SEPARATOR_PARAMETER = "Parameters:";
    Pattern PREPARING_PATTERN = Pattern.compile(SEPARATOR_PREPARING + "(.*?)(?=\n|\r|\r\n)");
    Pattern PARAMETER_PATTERN = Pattern.compile(SEPARATOR_PARAMETER + "(.*?)(?=\n|\r|\r\n)");
    Pattern END_WITH_PAREN = Pattern.compile("\\((.*?)\\)$");
}
