package xyz.msws.admintools.parsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import xyz.msws.admintools.Monitor;
import xyz.msws.admintools.data.ttt.TTTAction;

public class TTTParser extends Parser {

    private final List<TTTAction> tttActions = new ArrayList<>();
    private final Pattern pattern = Pattern.compile("^\\[\\d\\d:\\d\\d]\s"); // Matches the timecode prefix of TTT
                                                                             // logs
    private boolean parse = false;

    Set<String> lines = new HashSet<>();

    public TTTParser(Monitor monitor) {
        super(monitor);
    }

    @Override
    public void parse(String line) {
        if (line.equals("---------------TTT LOGS---------------") && !parse) {
            tttActions.clear();
            lines.clear();
            parse = true;
            return;
        }
        if (line.equals("--------------------------------------") && parse) {
            Arrays.stream(config.getHeader().split("\\\\n")).forEach(System.out::println);

            System.out.println("TTT Logs");
            for (TTTAction act : tttActions) {
                if (!config.getActions().contains(act.getType()))
                    continue;
                System.out.println(act.simplify());
            }
            tttActions.clear();
            lines.clear();
            parse = false;
            return;
        }
        if (!parse)
            return;
        if (!line.startsWith("["))
            return;
        if (line.charAt(8) == '[')
            return;
        if (line.endsWith("has been started!"))
            return;
        if (!pattern.matcher(line).lookingAt())
            return;

        try {
            TTTAction tttAction = new TTTAction(line);
            tttActions.add(tttAction);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unknown TTT line: " + line);
        }
    }

}
