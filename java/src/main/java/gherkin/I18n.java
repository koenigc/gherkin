package gherkin;

import gherkin.formatter.Formatter;
import gherkin.formatter.PrettyFormatter;
import gherkin.util.Mapper;

import java.io.StringWriter;
import java.util.*;

import static gherkin.util.FixJava.join;
import static gherkin.util.FixJava.map;

public class I18n {
    private static final List<String> ALL_KEYS = Arrays.asList("name", "native", "feature", "background", "scenario", "scenario_outline", "examples", "given", "when", "then", "and", "but");
    private static final List<String> KEYWORD_KEYS = Arrays.asList("feature", "background", "scenario", "scenario_outline", "examples", "given", "when", "then", "and", "but");
    private static final List<String> STEP_KEYWORD_KEYS = Arrays.asList("given", "when", "then", "and", "but");
    private static final Mapper QUOTE_MAPPER = new Mapper() {
        public String map(String string) {
            return '"' + string + '"';
        }
    };
    private static final Mapper CODE_KEYWORD_MAPPER = new Mapper() {
        public String map(String keyword) {
            return codeKeywordFor(keyword);
        }
    };

    private static String codeKeywordFor(String keyword) {
        return keyword.replaceAll("[\\s',]", "");
    }

    private final String isoCode;
    private final Locale locale;
    private final Map<String, List<String>> keywords;

    public I18n(String isoCode) {
        this.isoCode = isoCode;
        this.locale = localeFor(this.isoCode);
        this.keywords = new HashMap<String, List<String>>();

        populateKeywords();
    }

    private void populateKeywords() {
        ResourceBundle keywordBundle = ResourceBundle.getBundle("gherkin.I18nKeywords", locale);
        Enumeration<String> keys = keywordBundle.getKeys();
        while (keys.hasMoreElements()) {
            List<String> keywordList = new ArrayList<String>();
            String key = keys.nextElement();

            String value = keywordBundle.getString(key);
            for (String keyword : value.split("\\|")) {
                keywordList.add(keyword);
            }
            keywords.put(key, Collections.unmodifiableList(keywordList));
        }
    }

    public String getIsoCode() {
        return isoCode;
    }

    public Lexer lexer(Listener listener) {
        String qualifiedI18nLexerClassName = "gherkin.lexer." + locale.toString().toUpperCase();
        try {
            Class<?> delegateClass = Class.forName(qualifiedI18nLexerClassName);
            return (Lexer) delegateClass.getConstructor(Listener.class).newInstance(listener);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't load lexer class: " + qualifiedI18nLexerClassName, e);
        }
    }

    public List<String> keywords(String key) {
        return keywords.get(key);
    }

    public List<String> getCodeKeywords() {
        return map(getStepKeywords(), CODE_KEYWORD_MAPPER);
    }

    public List<String> getStepKeywords() {
        SortedSet<String> result = new TreeSet<String>();
        for (String keyword : STEP_KEYWORD_KEYS) {
            result.addAll(keywords.get(keyword));
        }
        return new ArrayList<String>(result);
    }

    public String getKeywordTable() {
        StringWriter writer = new StringWriter();
        Formatter pf = new PrettyFormatter(writer, true);
        for (String key : KEYWORD_KEYS) {
            pf.row(Arrays.asList(key, join(map(keywords(key), QUOTE_MAPPER), ", ")), 0);
        }
        for (String key : STEP_KEYWORD_KEYS) {
            List<String> codeKeywordList = new ArrayList<String>(keywords.get(key));
            codeKeywordList.remove("* ");
            String codeKeywords = join(map(map(codeKeywordList, CODE_KEYWORD_MAPPER), QUOTE_MAPPER), ", ");

            pf.row(Arrays.asList(key + " (code)", codeKeywords), 0);
        }
        pf.flushTable();
        return writer.getBuffer().toString();
    }

    private Locale localeFor(String isoString) {
        String[] languageAndCountry = isoString.split("-");
        if (languageAndCountry.length == 1) {
            return new Locale(isoString);
        } else {
            return new Locale(languageAndCountry[0], languageAndCountry[1]);
        }
    }
}
