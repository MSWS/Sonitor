package xyz.msws.admintools.parsers;

import xyz.msws.admintools.Monitor;
import xyz.msws.admintools.data.Action;
import xyz.msws.admintools.data.ActionType;
import xyz.msws.admintools.data.Role;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JBParser extends Parser {
    public JBParser(Monitor monitor) {
        super(monitor);
    }

    private final List<Action> actions = new ArrayList<>();
    private final Pattern pattern = Pattern.compile("^\\[\\d\\d:\\d\\d]\s");

    private final EnumSet<ActionType> wardenRelated = EnumSet.of(ActionType.WARDEN, ActionType.WARDEN_DEATH, ActionType.PASS, ActionType.FIRE);

    Set<String> lines = new HashSet<>();

    @Override
    public void parse(String line) {
        if (line.equals("----------------[ JAILBREAK LOGS ]----------------")) {
            actions.clear();
            return;
        }
        if (line.equals("--------------[ JAILBREAK LOGS END ]--------------")) {
            for (Action act : actions) {
                if (!config.getActions().contains(act.getType()))
                    continue;
                System.out.println(act.simplify());
            }

            System.out.println();

            System.out.println("Guard Freekills");
            checkGuardTime();
            System.out.println();
            System.out.println("Guard Vents");
            checkGuardVents();
            System.out.println();
            System.out.println("Game Buttons");
            checkWorldButtons();
            System.out.println();
            System.out.println("Nade Disruptions");
            checkNades();
            System.out.println();
            System.out.println("Gun Plants");
            checkGuns();
            System.out.println();
            System.out.println("Spectator Exploits");
            checkSpectator();
            actions.clear();
            lines.clear();
            return;
        }


        if (!line.startsWith("["))
            return;

        if (!pattern.matcher(line).lookingAt())
            return;

        Action action = new Action(line);
        actions.add(action);
    }

    private void checkGuardTime() {
        List<Action> allWarden = actions.stream().filter(act -> wardenRelated.contains(act.getType())).sorted().collect(Collectors.toList());
        TreeMap<Long, Boolean> cease = new TreeMap<>();

        for (Action act : allWarden) {
            switch (act.getType()) {
                case WARDEN -> cease.put(act.getTime() + 3, false);
                case WARDEN_DEATH -> cease.put(act.getTime(), true);
                case FIRE, PASS -> {
                    if (cease.entrySet().stream().anyMatch(e -> !e.getValue() && e.getKey() + 10 < act.getTime()))
                        break;
                    cease.put(act.getTime(), true);
                }
            }
        }

        Stream<Action> cs = actions.stream();
        List<Action> badCombat = cs.filter(act -> act.getType() == ActionType.DAMAGE || act.getType() == ActionType.KILL)
                .filter(act -> act.getPlayerRole() == Role.GUARD || act.getPlayerRole() == Role.WARDEN)
                .filter(act -> ceaseFire(cease, act.getTime())).filter(act -> act.getPlayerRole() == Role.PRISONER).collect(Collectors.toList());

        for (Action act : badCombat) {
            print(act.simplify() + " when there was no warden / time");
        }
    }

    private void checkGuardVents() {
        List<Action> breaks = new ArrayList<>();
        for (Action act : actions) {
            if (act.getType() != ActionType.VENTS)
                continue;
            if (act.getPlayerRole() == Role.PRISONER)
                break;
            breaks.add(act);
        }
        for (Action act : breaks) {
            print(act.simplify() + " before any prisoner did");
        }
    }

    private void checkWorldButtons() {
        List<Action> presses = actions.stream().filter(act -> act.getType() == ActionType.BUTTON).collect(Collectors.toList());

        for (Action act : actions.stream().filter(act -> act.getPlayerRole() == Role.WORLD).collect(Collectors.toList())) {
            List<Action> press = presses.stream().filter(p -> p.getTime() <= act.getTime() && p.getTime() > act.getTime() - config.getButtonTimeout()).collect(Collectors.toList());
            for (Action p : press) {
                print(p.simplify() + " which could've " + (act.getType() == ActionType.DAMAGE ? "damaged" : "killed") + " " + act.getTarget() + " (" + act.getTargetRole().getIcon() + ")");
            }
        }
    }

    private void checkNades() {
        List<Action> nades = actions.stream().filter(act -> act.getType() == ActionType.NADE).collect(Collectors.toList());

        for (Action act : actions.stream().filter(act -> act.getPlayerRole() == Role.WORLD).collect(Collectors.toList())) {
            List<Action> press = nades.stream().filter(p -> p.getTime() <= act.getTime() && p.getTime() > act.getTime() - config.getNadeTimeout()).collect(Collectors.toList());
            for (Action p : press) {
                print(p.simplify() + " which could've disrupted " + act.getTarget() + " (" + act.getTargetRole().getIcon() + ")");
            }
        }
    }

    private void checkGuns() {
        Map<String, List<Action>> drops = new HashMap<>();

        List<Action> guns = actions.stream().filter(act -> act.getType() == ActionType.DROP_WEAPON).collect(Collectors.toList());
        for (Action act : guns) {
            String gun = act.getOther()[0];
            List<Action> related = drops.getOrDefault(gun, new ArrayList<>());
            related.add(act);
            drops.put(gun, related);
        }

        for (Action act : actions.stream().filter(act -> act.getType() == ActionType.DAMAGE).collect(Collectors.toList())) {
            List<Action> ds = guns.stream().filter(p -> p.getTime() <= act.getTime() && p.getTime() > act.getTime() - config.getGunTimeout() && p.getOther()[0].equals(act.getOther()[1])).collect(Collectors.toList());
            for (Action p : ds) {
                print(p.simplify() + " and " + act.getTarget() + " (" + act.getTargetRole().getIcon() + ") used one shortly after");
            }
        }
    }

    private void checkSpectator() {
        List<Action> damage = actions.stream().filter(act -> act.getType() == ActionType.DAMAGE || act.getType() == ActionType.KILL)
                .filter(act -> act.getPlayerRole() == Role.SPECTATOR).collect(Collectors.toList());
        for (Action act : damage) {
            print(act.simplify() + " as a spectator");
        }
    }

    private boolean ceaseFire(TreeMap<Long, Boolean> times, long time) {
        List<Long> ts = new ArrayList<>(times.keySet());
        for (int i = 0; i < ts.size(); i++) {
            if (time >= ts.get(i) && (i == ts.size() - 1 || time < ts.get(i + 1)))
                return times.get(ts.get(i));
        }
        return false;
    }

    private void print(String line) {
        if (lines.contains(line))
            return;
        System.out.println(line);
        lines.add(line);
    }

}
