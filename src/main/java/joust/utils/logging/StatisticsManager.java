package joust.utils.logging;

import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class StatisticsManager {
    private HashMap<String, Integer> counters = new HashMap<String, Integer>();

    private Set<String> modifiedFiles = new HashSet<String>();

    // Tracked to aid in pretty-printing.
    private int longestKeyLength;

    public void increment(String key) {
        if (key.length() > longestKeyLength) {
            longestKeyLength = key.length();
        }

        if (!counters.containsKey(key)) {
            counters.put(key, 1);
            return;
        }

        counters.put(key, counters.get(key) + 1);
    }

    public void touchedFile(Env<AttrContext> currentEnv) {
        modifiedFiles.add(currentEnv.toplevel.sourcefile.getName());
    }

    public void printStatistics() {
        log.info("--------------------");
        for (String s : counters.keySet()) {
            log.info(String.format("%-" + (longestKeyLength + 4) + "s %d", s, counters.get(s)));
        }
        log.info("--------------------");
        for (String modifiedFile : modifiedFiles) {
            log.info("Modified: {}", modifiedFile);
        }
    }
}
