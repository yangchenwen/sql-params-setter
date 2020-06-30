package util;

import bean.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.hibernate.engine.jdbc.internal.Formatter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static constants.Const.*;

/**
 * @author yangchenwen
 * @since 2020-06-17 08:38:17
 */
public class SqlUtil {

    private SqlUtil() {
    }

    private static final Formatter FORMATTER = new BasicFormatterImpl();

    private static final Map<String, String> SQL_FORMAT_KW = new HashMap<>();

    static {
        String lineSeparator = System.lineSeparator();
        SQL_FORMAT_KW.put("(?i)\\s+select\\s+", "select" + lineSeparator + "\t");
        SQL_FORMAT_KW.put("(?i)\\s+from\\s+", lineSeparator + "from ");
        SQL_FORMAT_KW.put("(?i)\\s+where\\s+", lineSeparator + "where ");
        SQL_FORMAT_KW.put("(?i)\\s+and\\s+", lineSeparator + "\tand ");
        SQL_FORMAT_KW.put("(?i)\\s+or\\s+", lineSeparator + "\tor ");
        SQL_FORMAT_KW.put("(?i)\\s+left join\\s+", lineSeparator + "\tleft join ");
        SQL_FORMAT_KW.put("(?i)\\s+right join\\s+", lineSeparator + "\tright join ");
        SQL_FORMAT_KW.put("(?i)\\s+inner join\\s+", lineSeparator + "\tinner join ");
        SQL_FORMAT_KW.put("(?i)\\s+order by\\s+", lineSeparator + "order by ");
        SQL_FORMAT_KW.put("(?i)\\s+group by\\s+", lineSeparator + "group by ");
    }

    /**
     * simply sql format
     *
     * @param sql the executable sql
     * @return formatted executable sql
     */
    public static String simpleFormat(String sql) {
        if (StringUtils.isBlank(sql)) {
            return StringUtils.EMPTY;
        }

        for (Map.Entry<String, String> entry : SQL_FORMAT_KW.entrySet()) {
            sql = sql.replaceAll(entry.getKey(), entry.getValue());
        }

        return sql.endsWith(SEMICOLON) ? sql : sql + SEMICOLON;
    }

    /**
     * using Hibernate formatter
     *
     * @param sql the executable sql
     * @return formatted executable sql
     */
    public static String format(String sql) {
        if (StringUtils.isBlank(sql)) {
            return StringUtils.EMPTY;
        }

        sql = FORMATTER.format(sql);

        return sql.endsWith(SEMICOLON) ? sql : sql + SEMICOLON;
    }


    /**
     * parsing the selected mybatis logs
     * e.g. <p>{@code ==> Preparing: select * from table where id = ?}</p>
     * <p>{@code ==> Parameters: 123(String)}</p>
     * to extract an executable sql
     *
     * @param mybatisLogs the selected mybatis logs
     * @return an executable sql
     */
    public static String parse(String mybatisLogs) {
        if (StringUtils.isBlank(mybatisLogs)) {
            return StringUtils.EMPTY;
        }

        String preparedSql = StringUtils.EMPTY;
        Matcher preparingSqlMatcher = PREPARING_PATTERN.matcher(mybatisLogs);
        if (preparingSqlMatcher.find()) {
            preparedSql = preparingSqlMatcher.group(1).trim();
        }

        Matcher paramsMatcher = PARAMETER_PATTERN.matcher(mybatisLogs);
        if (paramsMatcher.find()) {
            String params = paramsMatcher.group(1);
            if (StringUtils.isBlank(params)) {
                return format(preparedSql);
            }

            String[] paramsArr = StringUtils.split(params, COMMA);
            for (String param : paramsArr) {
                String value = Parameter.of(param).getValue();
                preparedSql = StringUtils.replaceOnce(preparedSql, PALACE_HOLDER, value);
            }
        }

        return format(preparedSql);
    }
}
