package constants;

import java.util.regex.Pattern;

/**
 * @author yangchenwen
 * @since 2020-06-09 15:27:41
 */
public interface Const {

    String PALACE_HOLDER = "?";
    String SEPARATOR_PREPARING = "Preparing:";
    String SEPARATOR_PARAMETER = "Parameters:";
    String PARAM_TYPE_REGEX = "\\((.*?)\\)";
    Pattern PREPARING_PATTERN = Pattern.compile(SEPARATOR_PREPARING + "(.*?)$");
    Pattern PARAMETER_PATTERN = Pattern.compile(SEPARATOR_PARAMETER + "(.*?)$");
}
