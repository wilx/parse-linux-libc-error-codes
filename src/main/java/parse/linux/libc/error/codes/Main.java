package parse.linux.libc.error.codes;

import org.anarres.cpp.DefaultPreprocessorListener;
import org.anarres.cpp.Feature;
import org.anarres.cpp.LexerException;
import org.anarres.cpp.Macro;
import org.anarres.cpp.StringLexerSource;
import org.anarres.cpp.Token;
import org.anarres.cpp.Warning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map.Entry;

public class Main {
    private final Logger LOG;

    public Main() {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.err");
        LOG = LoggerFactory.getLogger(Main.class);
    }

    public static void main(String[] args) throws LexerException, IOException {
        (new Main()).run(args);
    }

    void run(String[] args) throws LexerException, IOException {
        try (OurPreprocessor pp = new OurPreprocessor()) {
            preprocess(pp);
        }

    }

    void preprocess(OurPreprocessor pp) throws LexerException, IOException {
        pp.addFeature(Feature.DIGRAPHS);
        pp.addFeature(Feature.TRIGRAPHS);
        pp.addFeature(Feature.LINEMARKERS);
        pp.addFeature(Feature.INCLUDENEXT);
        pp.addFeature(Feature.DEBUG);

        pp.addWarning(Warning.IMPORT);

        pp.setListener(new DefaultPreprocessorListener());

        pp.addMacro("__JCPP__");

        pp.getSystemIncludePath().add("/usr/include");

        pp.addInput(new StringLexerSource("#include <errno.h>"));

        try {
            for (; ; ) {
                Token tok = pp.token();
                if (tok == null) {
                    break;
                }
                if (tok.getType() == Token.EOF) {
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error("Preprocessor failed.", e);
            throw e;
        }

        pp.getMacros().entrySet().stream()
            .filter(e -> e.getKey().startsWith("E")
                         && !e.getValue().isFunctionLike())
            .sorted(new CompareMacroByValue())
            .map(e -> "int " + e.getKey() + " = " + e.getValue().getText() + ";")
            .forEach(System.out::println);
    }

    static class CompareMacroByValue implements Comparator<Entry<String, Macro>> {
        @Override
        public int compare(Entry<String, Macro> o1, Entry<String, Macro> o2) {
            String val1 = o1.getValue().getText();
            String val2 = o2.getValue().getText();

            try {
                int num1 = Integer.parseInt(val1);
                int num2 = Integer.parseInt(val2);
                return num1 - num2;
            } catch (NumberFormatException ex) {
                // Fall-through to string comparison.
            }

            return val1.compareTo(val2);
        }
    }
}
