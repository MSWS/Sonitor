package xyz.msws.admintools.parsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import xyz.msws.admintools.Monitor;
import xyz.msws.admintools.data.DataStructs.GenericActionType;
import xyz.msws.admintools.data.ttt.TTTAction;
import xyz.msws.admintools.data.ttt.TTTActionType;
import xyz.msws.admintools.data.ttt.TTTRole;

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
                try {
                    System.out.println(act.simplify());
                } catch (Exception e) {
                    System.out.println("Error occured while parsing " + act.getLine() + ":\n" + e.getMessage());
                }
            }

            checkHidden();
            checkRDM();

            tttActions.clear();
            lines.clear();
            parse = false;
            return;
        }
        if (!parse)
            return;
        if (!line.startsWith("[") || line.charAt(8) != '-' || line.charAt(9) != '>')
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

    private void checkHidden() {
        Set<String> revealed = new HashSet<>();
        Set<String> toPrint = new HashSet<>();
        for (TTTAction act : tttActions) {
            if (act.getPlayerRole() == TTTRole.TRAITOR
                    && (act.getType() == GenericActionType.DAMAGE || act.getType() == TTTActionType.T_SECRET
                            || act.getType() == TTTActionType.TAZE)) {
                revealed.add(act.getType() == TTTActionType.TAZE ? act.getOther()[0] : act.getPlayer());
                continue;
            }

            if (act.getTargetRole() != TTTRole.TRAITOR
                    || (act.getType() != GenericActionType.DAMAGE && act.getType() != GenericActionType.KILL))
                continue;

            if (revealed.contains(act.getTarget()))
                continue;

            toPrint.add(act.simplify());
        }
        if (toPrint.isEmpty())
            return;
        print("\nEarly Traitor Kills (Ts did not commit any acts)");
        toPrint.forEach(s -> print(s));
    }

    private void checkRDM() {
        Set<String> toPrint = new HashSet<>();
        for (TTTAction act : tttActions) {
            if (act.getPlayerRole() != act.getTargetRole() && !act.isBadAction())
                continue;
            if (act.getType() == TTTActionType.BAD_DAMAGE)
                continue;
            toPrint.add(act.simplify());
        }
        if (toPrint.isEmpty())
            return;
        print("\nBad Actions");
        toPrint.forEach(s -> print(s));
    }

    private void print(String line) {
        if (lines.contains(line))
            return;
        System.out.println(line);
        lines.add(line);
    }

}
