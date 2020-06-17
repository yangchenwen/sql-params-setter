package util;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yangchenwen
 * @since 2020-06-17 08:38:17
 */
public class SqlUtil {

    private SqlUtil() {
    }

    private static final Map<String, String> SQL_FORMAT_KW = new HashMap<>();

    static {
        String lineSeparator = System.lineSeparator();
        SQL_FORMAT_KW.put(",", "," + lineSeparator + "\t");
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

    public static String format(String sql) {
        if (StringUtils.isBlank(sql)) {
            return StringUtils.EMPTY;
        }

        for (Map.Entry<String, String> entry : SQL_FORMAT_KW.entrySet()) {
            sql = sql.replaceAll(entry.getKey(), entry.getValue());
        }

        return sql.endsWith(";") ? sql : sql + ";";
    }
}
