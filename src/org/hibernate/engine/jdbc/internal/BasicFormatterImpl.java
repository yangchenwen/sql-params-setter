/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.util.*;

/**
 * Original version of this implementation please refer to
 * <a href="https://github.com/hibernate/hibernate-orm/blob/a30635f14ae272fd63a653f9a9e1a9aeb390fad4/hibernate-core/src/main/java/org/hibernate/engine/jdbc/internal/BasicFormatterImpl.java">here</a>.
 * <p>
 * Performs formatting of basic SQL statements (DML + query).
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BasicFormatterImpl implements Formatter {

    private static final String WHITESPACE = " \n\r\f\t";

    private static final Set<String> BEGIN_CLAUSES = new HashSet<>();
    private static final Set<String> END_CLAUSES = new HashSet<>();
    private static final Set<String> LOGICAL = new HashSet<>();
    private static final Set<String> QUANTIFIERS = new HashSet<>();
    private static final Set<String> DML = new HashSet<>();
    private static final Set<String> MISC = new HashSet<>();

    static {
        BEGIN_CLAUSES.add("left");
        BEGIN_CLAUSES.add("right");
        BEGIN_CLAUSES.add("inner");
        BEGIN_CLAUSES.add("outer");
        BEGIN_CLAUSES.add("group");
        BEGIN_CLAUSES.add("order");

        END_CLAUSES.add("where");
        END_CLAUSES.add("set");
        END_CLAUSES.add("having");
        END_CLAUSES.add("join");
        END_CLAUSES.add("from");
        END_CLAUSES.add("by");
        END_CLAUSES.add("into");
        END_CLAUSES.add("union");

        LOGICAL.add("and");
        LOGICAL.add("or");
        LOGICAL.add("when");
        LOGICAL.add("else");
        LOGICAL.add("end");

        QUANTIFIERS.add("in");
        QUANTIFIERS.add("all");
        QUANTIFIERS.add("exists");
        QUANTIFIERS.add("some");
        QUANTIFIERS.add("any");

        DML.add("insert");
        DML.add("update");
        DML.add("delete");

        MISC.add("select");
        MISC.add("on");
    }

    private static final String INDENT_STRING = "    ";
    private static final String INITIAL = System.lineSeparator() + "";

    @Override
    public String format(String source) {
        return new FormatProcess(source).perform();
    }

    private static class FormatProcess {
        boolean beginLine = true;
        boolean afterBeginBeforeEnd;
        boolean afterByOrSetOrFromOrSelect;
        boolean afterValues;
        boolean afterOn;
        boolean afterBetween;
        boolean afterInsert;
        int inFunction;
        int parensSinceSelect;
        private final LinkedList<Integer> parenCounts = new LinkedList<>();
        private final LinkedList<Boolean> afterByOrFromOrSelects = new LinkedList<>();

        int indent = 0;

        StringBuilder result = new StringBuilder();
        StringTokenizer tokens;
        String lastToken;
        String token;
        String lcToken;

        public FormatProcess(String sql) {
            tokens = new StringTokenizer(
                    sql,
                    "()+*/-=<>'`\"[]," + WHITESPACE,
                    true
            );
        }

        public String perform() {

            result.append(INITIAL);

            while (tokens.hasMoreTokens()) {
                token = tokens.nextToken();
                lcToken = token.toLowerCase(Locale.ROOT);

                switch (token) {
                    case "'": {
                        String t;
                        do {
                            t = tokens.nextToken();
                            token += t;
                        }
                        // cannot handle single quotes
                        while (!"'".equals(t) && tokens.hasMoreTokens());
                        break;
                    }
                    case "\"": {
                        String t;
                        do {
                            t = tokens.nextToken();
                            token += t;
                        }
                        while (!"\"".equals(t) && tokens.hasMoreTokens());
                        break;
                    }
                    // SQL Server uses "[" and "]" to escape reserved words
                    // see SQLServerDialect.openQuote and SQLServerDialect.closeQuote
                    case "[": {
                        String t;
                        do {
                            t = tokens.nextToken();
                            token += t;
                        }
                        while (!"]".equals(t) && tokens.hasMoreTokens());
                        break;
                    }
                }

                ProcessEnum[] processEnums = ProcessEnum.values();
                for (ProcessEnum pe : processEnums) {
                    if (pe.process(this)) {
                        break;
                    }
                }

                if (!ProcessEnum.isWhitespace(token)) {
                    lastToken = lcToken;
                }
            }
            return result.toString();
        }

        private void misc() {
            out();
            if ("between".equals(lcToken)) {
                afterBetween = true;
            }
            if (afterInsert) {
                newline();
                afterInsert = false;
            } else {
                beginLine = false;
                if ("case".equals(lcToken)) {
                    indent++;
                }
            }
        }

        private void out() {
            result.append(token);
        }

        private void newline() {
            result.append(System.lineSeparator());
            for (int i = 0; i < indent; i++) {
                result.append(INDENT_STRING);
            }
            beginLine = true;
        }

        private enum ProcessEnum {

            PROCESS_COMMA_AFTER_BY_FROM_SELECT {
                @Override
                public boolean process(FormatProcess p) {
                    if (p.afterByOrSetOrFromOrSelect && ",".equals(p.token)) {
                        p.out();
                        p.newline();
                        return true;
                    }
                    return false;
                }
            },

            PROCESS_COMMA_AFTER_ON {
                @Override
                public boolean process(FormatProcess p) {
                    if (p.afterOn && ",".equals(p.token)) {
                        p.out();
                        p.indent--;
                        p.newline();
                        p.afterOn = false;
                        p.afterByOrSetOrFromOrSelect = true;
                        return true;
                    }
                    return false;
                }
            },

            PROCESS_OPEN_PAREN {
                @Override
                public boolean process(FormatProcess p) {
                    if ("(".equals(p.token)) {
                        if (isFunctionName(p.lastToken) || p.inFunction > 0) {
                            p.inFunction++;
                        }
                        p.beginLine = false;
                        if (p.inFunction > 0) {
                            p.out();
                        } else {
                            p.out();
                            if (!p.afterByOrSetOrFromOrSelect) {
                                p.indent++;
                                p.newline();
                                p.beginLine = true;
                            }
                        }
                        p.parensSinceSelect++;

                        return true;
                    }
                    return false;
                }
            },

            PROCESS_CLOSE_PAREN {
                @Override
                public boolean process(FormatProcess p) {
                    if (")".equals(p.token)) {
                        p.parensSinceSelect--;
                        if (p.parensSinceSelect < 0) {
                            p.indent--;
                            p.parensSinceSelect = p.parenCounts.removeLast();
                            p.afterByOrSetOrFromOrSelect = p.afterByOrFromOrSelects.removeLast();
                        }
                        if (p.inFunction > 0) {
                            p.inFunction--;
                        } else {
                            if (!p.afterByOrSetOrFromOrSelect) {
                                p.indent--;
                                p.newline();
                            }
                        }
                        p.out();
                        p.beginLine = false;

                        return true;
                    }
                    return false;
                }
            },

            PROCESS_BEGIN_NEW_CLAUSE {
                @Override
                public boolean process(FormatProcess p) {
                    if (BEGIN_CLAUSES.contains(p.lcToken)) {
                        if (!p.afterBeginBeforeEnd) {
                            if (p.afterOn) {
                                p.indent--;
                                p.afterOn = false;
                            }
                            p.indent--;
                            p.newline();
                        }
                        p.out();
                        p.beginLine = false;
                        p.afterBeginBeforeEnd = true;
                        return true;
                    }
                    return false;
                }
            },

            PROCESS_END_NEW_CLAUSE {
                @Override
                public boolean process(FormatProcess p) {
                    if (END_CLAUSES.contains(p.lcToken)) {
                        if (!p.afterBeginBeforeEnd) {
                            p.indent--;
                            if (p.afterOn) {
                                p.indent--;
                                p.afterOn = false;
                            }
                            p.newline();
                        }
                        p.out();
                        if (!"union".equals(p.lcToken)) {
                            p.indent++;
                        }
                        p.newline();
                        p.afterBeginBeforeEnd = false;
                        p.afterByOrSetOrFromOrSelect = "by".equals(p.lcToken)
                                || "set".equals(p.lcToken)
                                || "from".equals(p.lcToken);
                        return true;
                    }
                    return false;
                }
            },

            PROCESS_SELECT {
                @Override
                public boolean process(FormatProcess p) {
                    if ("select".equals(p.lcToken)) {
                        p.out();
                        p.indent++;
                        p.newline();
                        p.parenCounts.addLast(p.parensSinceSelect);
                        p.afterByOrFromOrSelects.addLast(p.afterByOrSetOrFromOrSelect);
                        p.parensSinceSelect = 0;
                        p.afterByOrSetOrFromOrSelect = true;
                        return true;
                    }
                    return false;
                }
            },

            PROCESS_UPDATE_INSERT_DELETE {
                @Override
                public boolean process(FormatProcess p) {
                    if (DML.contains(p.lcToken)) {
                        p.out();
                        p.indent++;
                        p.beginLine = false;
                        if ("update".equals(p.lcToken)) {
                            p.newline();
                        }
                        if ("insert".equals(p.lcToken)) {
                            p.afterInsert = true;
                        }
                        return true;
                    }
                    return false;
                }
            },

            PROCESS_VALUES {
                @Override
                public boolean process(FormatProcess p) {
                    if ("values".equals(p.lcToken)) {
                        p.indent--;
                        p.newline();
                        p.out();
                        p.indent++;
                        p.newline();
                        p.afterValues = true;
                        return true;
                    }
                    return false;
                }
            },

            PROCESS_ON {
                @Override
                public boolean process(FormatProcess p) {
                    if ("on".equals(p.lcToken)) {
                        p.indent++;
                        p.afterOn = true;
                        p.newline();
                        p.out();
                        p.beginLine = false;
                        return true;
                    }
                    return false;
                }
            },

            PROCESS_AFTER_BETWEEN_AND {
                @Override
                public boolean process(FormatProcess p) {
                    if (p.afterBetween && p.lcToken.equals("and")) {
                        p.misc();
                        p.afterBetween = false;
                        return true;
                    }
                    return false;
                }
            },

            PROCESS_LOGICAL {
                @Override
                public boolean process(FormatProcess p) {
                    if (LOGICAL.contains(p.lcToken)) {
                        if ("end".equals(p.lcToken)) {
                            p.indent--;
                        }
                        p.newline();
                        p.out();
                        p.beginLine = false;
                        return true;
                    }
                    return false;
                }
            },

            PROCESS_WHITE_SPACE {
                @Override
                public boolean process(FormatProcess p) {
                    if (isWhitespace(p.token)) {
                        if (!p.beginLine) {
                            p.result.append(" ");
                        }
                        return true;
                    }
                    return false;
                }
            },

            PROCESS_MISC {
                @Override
                public boolean process(FormatProcess p) {
                    p.misc();
                    return true;
                }
            };

            abstract public boolean process(FormatProcess p);

            private static boolean isFunctionName(String tok) {
                if (tok == null || tok.length() == 0) {
                    return false;
                }

                final char begin = tok.charAt(0);
                final boolean isIdentifier = Character.isJavaIdentifierStart(begin) || '"' == begin;
                return isIdentifier &&
                        !LOGICAL.contains(tok) &&
                        !END_CLAUSES.contains(tok) &&
                        !QUANTIFIERS.contains(tok) &&
                        !DML.contains(tok) &&
                        !MISC.contains(tok);
            }

            private static boolean isWhitespace(String token) {
                return WHITESPACE.contains(token);
            }
        }
    }

}