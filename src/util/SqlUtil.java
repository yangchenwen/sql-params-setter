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
        SQL_FORMAT_KW.put("(?i)select", "select" + lineSeparator + "\t");
        SQL_FORMAT_KW.put("(?i)from", lineSeparator + "from");
        SQL_FORMAT_KW.put("(?i)where", lineSeparator + "where");
        SQL_FORMAT_KW.put("(?i)and", lineSeparator + "\tand");
        SQL_FORMAT_KW.put("(?i)or", lineSeparator + "\tor");
        SQL_FORMAT_KW.put("(?i)left join", lineSeparator + "\tleft join");
        SQL_FORMAT_KW.put("(?i)right join", lineSeparator + "\tright join");
        SQL_FORMAT_KW.put("(?i)inner join", lineSeparator + "\tinner join");
        SQL_FORMAT_KW.put("(?i)order by", lineSeparator + "order by");
        SQL_FORMAT_KW.put("(?i)group by", lineSeparator + "group by");
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
