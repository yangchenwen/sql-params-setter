package actions;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static constants.Const.*;

/**
 * @author yangchenwen
 * @since 2020-06-09 15:27:40
 */
public class PreparedSqlParamsSetterAction extends AnAction {

    private static final NotificationGroup NOTIFICATION_GROUP =
            new NotificationGroup("SqlParamsSetter.NotificationGroup", NotificationDisplayType.BALLOON, true);

    @Override
    public void actionPerformed(AnActionEvent e) {

        final Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        Project project = editor.getProject();
        if (project == null) {
            return;
        }

        SelectionModel model = editor.getSelectionModel();
        final String selectedMybatisLogs = model.getSelectedText();
        if (StringUtils.isBlank(selectedMybatisLogs)) {
            return;
        }

        String sql;
        String message;
        try {
            sql = extractSqlAndSetParams(selectedMybatisLogs);
        } catch (Exception ex) {
            message = String.format("Failed at: %s", ex.toString());
            Notification error = NOTIFICATION_GROUP.createNotification(message, NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
            return;
        }

        if (StringUtils.isBlank(sql)) {
            message = String.format("Selected area should contain both [%s] in the 1st line and [%s] in the 2nd line.",
                    SEPARATOR_PREPARING, SEPARATOR_PARAMETER);
            Notification warning = NOTIFICATION_GROUP.createNotification(message, NotificationType.WARNING);
            Notifications.Bus.notify(warning, project);
            return;
        }

        StringSelection selection = new StringSelection(sql);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
        message = "Success, copied to clipboard.";
        Notification success = NOTIFICATION_GROUP.createNotification(message, NotificationType.INFORMATION);
        Notifications.Bus.notify(success, project);
    }

    private String extractSqlAndSetParams(String selectedMybatisLogs) {
        String[] mybatisSqlLogs = StringUtils.split(selectedMybatisLogs, System.lineSeparator());
        if (mybatisSqlLogs == null) {
            return StringUtils.EMPTY;
        }

        int logLines = mybatisSqlLogs.length;
        if (logLines < 1) {
            return StringUtils.EMPTY;
        }

        String sqlLine = mybatisSqlLogs[0];
        if (StringUtils.isBlank(sqlLine)) {
            return StringUtils.EMPTY;
        }
        String preparedSql = StringUtils.EMPTY;
        Matcher matcher = PREPARING_PATTERN.matcher(sqlLine);
        if (matcher.find()) {
            String group = matcher.group();
            preparedSql = StringUtils.replace(group, SEPARATOR_PREPARING, StringUtils.EMPTY);
        }

        if (logLines < 2) {
            return preparedSql.trim();
        }

        String paramLine = mybatisSqlLogs[1];
        Matcher paramsMatcher = PARAMETER_PATTERN.matcher(paramLine);
        if (paramsMatcher.find()) {
            String params = StringUtils.replace(paramsMatcher.group(), SEPARATOR_PARAMETER, StringUtils.EMPTY);
            String[] paramsArr = StringUtils.split(params, ",");
            for (String param : paramsArr) {
                String replacement = param.replaceAll(PARAM_TYPE_REGEX, StringUtils.EMPTY);
                if (param.contains("String") || param.contains("Date")
                        || param.contains("Timestamp") || param.contains("Time")) {
                    replacement = "'" + replacement.trim() + "'";
                }
                preparedSql = StringUtils.replaceOnce(preparedSql, PALACE_HOLDER, replacement);
            }
        }

        return preparedSql.trim();
    }

    public static void main(String[] args) {
        Pattern pattern = Pattern.compile("Preparing:(.*?)$");
        Matcher matcher = pattern.matcher("==>  Preparing: select count(0) from dual");

        if (matcher.find()) {
            System.out.println(matcher.group());
        }

        System.out.println("abc(String)".replaceAll("\\((.*?)\\)", ""));
    }
}
