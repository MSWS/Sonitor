package xyz.msws.admintools.parsers;

import xyz.msws.admintools.Monitor;
import xyz.msws.admintools.data.*;
import xyz.msws.admintools.utils.MSG;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JBParser extends Parser {

    private final ButtonDatabase buttondb;

    public JBParser(Monitor monitor) {
        super(monitor);

        File master = new File(System.getProperty("user.dir"));
        File buttons = new File(master, "buttons.txt");
        this.buttondb = new ButtonDatabase(buttons);
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
            Arrays.stream(config.getHeader().split("\\\\n")).forEach(System.out::println);

            System.out.println("Jailbreak Logs");
            for (Action act : actions) {
                if (!config.getActions().contains(act.getType()))
                    continue;
                System.out.println(act.simplify());
            }

            if (config.showEarlyKills())
                checkGuardTime();
            if (config.showEarlyVents())
                checkGuardVents();
            if (config.showGameButtons())
                checkWorldButtons();
            if (config.showNades())
                checkNades();
            if (config.showGunPlants())
                checkGuns();
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
                case WARDEN -> cease.put(act.getTime() + config.getWardenTimeout(), false);
                case WARDEN_DEATH -> cease.put(act.getTime(), true);
                case FIRE, PASS -> {
                    if (cease.entrySet().stream().anyMatch(e -> !e.getValue() && e.getKey() + config.getFreeTime() < act.getTime()))
                        break;
                    cease.put(act.getTime(), true);
                }
            }
        }
        Set<String> cts = new HashSet<>();
        Set<String> ts = new HashSet<>();
        for (Action act : actions) {
            if (act.getPlayerRole().isT())
                ts.add(act.getPlayer());
            if (act.getTargetRole() != null && act.getTargetRole().isT())
                ts.add(act.getTarget());
            if (act.getPlayerRole().isCT())
                cts.add(act.getPlayer());
            if (act.getTargetRole() != null && act.getTargetRole().isCT())
                cts.add(act.getTarget());
        }
        List<Action> deaths = actions.stream().filter(act -> act.getType() == ActionType.KILL).collect(Collectors.toList());
        if (!deaths.isEmpty()) {
            boolean ctWin = deaths.get(deaths.size() - 1).getPlayerRole().isCT();
            List<Action> ctDeaths = deaths.stream().filter(act -> act.getTargetRole().isCT()).sorted().collect(Collectors.toList());
            List<Action> tDeaths = deaths.stream().filter(act -> act.getTargetRole().isT()).sorted().collect(Collectors.toList());
            Action lastGuard, lastRequest = null;
            if (tDeaths.size() > 2 && ts.size() - tDeaths.size() <= 2) {
                lastRequest = tDeaths.get(tDeaths.size() - 2);
                cease.put(lastRequest.getTime(), false);
                System.out.println(lastRequest.getTarget() + " died, activating last request at " + lastRequest.getTimeString());
            }
            if (ctDeaths.size() > 2 && cts.size() - ctDeaths.size() <= 1) {
                // There was one CT at the end of the round
                // If CT won, get most recent death (the last CT didn't die), otherwise get second most recent death
                lastGuard = ctDeaths.get(ctDeaths.size() - (ctWin ? 1 : 2));
                if (lastRequest == null || lastRequest.getTime() > lastGuard.getTime()) {
                    cease.put(lastGuard.getTime(), false);
                    System.out.println(lastGuard.getTarget() + " died, activating last guard at " + lastGuard.getTimeString());
                }
            }
        }


        List<Action> badCombat = actions.stream().filter(act -> act.getType() == ActionType.DAMAGE || act.getType() == ActionType.KILL)
                .filter(act -> act.getPlayerRole() == Role.GUARD || act.getPlayerRole() == Role.WARDEN).filter(act -> act.getTargetRole() == Role.PRISONER)
                .filter(act -> ceaseFire(cease, act.getTime())).collect(Collectors.toList());

        if (!badCombat.isEmpty())
            print("\nGuard Freekills");
        for (Action act : badCombat) {
            int seconds = secondsToCease(cease, act.getTime());
            if (seconds == -1) {
                print(act.simplify() + " with no warden.");
                continue;
            }
            print(act.simplify() + " within " + seconds + MSG.plural("second", seconds) + " of new warden");
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
        if (!breaks.isEmpty())
            print("\nEarly Guard Vents");
        for (Action act : breaks) {
            print(act.simplify() + " before any prisoner did");
        }
    }

    private void checkWorldButtons() {
        List<Action> presses = actions.stream().filter(act -> act.getType() == ActionType.BUTTON).collect(Collectors.toList());
        List<Action> damages = actions.stream().filter(act -> act.getPlayerRole() == Role.WORLD).collect(Collectors.toList());

        List<String> lines = new ArrayList<>();
        for (Action press : presses) {
            Button button = buttondb.getButton(press.getOther()[0]);
            if (button.isSafe())
                return;
            StringBuilder result = new StringBuilder();
            List<Action> damaged = damages.stream().filter(d -> d.getTime() >= press.getTime() && d.getTime() < press.getTime() + config.getButtonTimeout()).collect(Collectors.toList());
            if (damaged.isEmpty())
                continue;
            result.append(press.simplify());
            result.append(" which could've harmed ");
            Set<String> players = new HashSet<>();
            int t = 0, ct = 0;
            for (Action act : damaged) {
                if (players.contains(act.getTarget()))
                    continue;
                players.add(act.getTarget());
                if (act.getTargetRole().isT())
                    t++;
                if (act.getTargetRole().isCT())
                    ct++;
            }
            if (t > 0) {
                result.append(t).append(" ").append(MSG.plural("Prisoner", t));
                if (ct > 0)
                    result.append(" and ");
            }
            if (ct > 0)
                result.append(ct).append(" ").append(MSG.plural("CT", ct));
            lines.add(result.toString());
        }
        if (!lines.isEmpty()) {
            print("\nGame Buttons");

            for (String line : lines)
                print(line);
        }
    }

    private void checkNades() {
        List<Action> nades = actions.stream().filter(act -> act.getType() == ActionType.NADE).collect(Collectors.toList());

        for (Action act : actions.stream().filter(act -> act.getPlayerRole() == Role.WORLD).collect(Collectors.toList())) {
            List<Action> press = nades.stream().filter(p -> p.getTime() <= act.getTime() && p.getTime() > act.getTime() - config.getNadeTimeout())
                    .filter(p -> !act.getTarget().equals(p.getPlayer())).collect(Collectors.toList());
            for (Action p : press) {
                print("\nNade Disruptions");
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

        for (Action act : actions.stream().filter(act -> act.getType() == ActionType.DAMAGE && act.getPlayerRole().isT()).collect(Collectors.toList())) {
            List<Action> ds = guns.stream().filter(p -> p.getTime() <= act.getTime() && p.getTime() > act.getTime() - config.getGunTimeout() && p.getOther()[0].equals(act.getOther()[1])).collect(Collectors.toList());
            for (Action p : ds) {
                print("\nGun Plants");
                print(p.simplify() + " and " + act.getPlayer() + " (" + act.getPlayerRole().getIcon() + ") used one shortly after");
            }
        }
    }

    private void checkSpectator() {
        List<Action> damage = actions.stream().filter(act -> act.getType() == ActionType.DAMAGE || act.getType() == ActionType.KILL)
                .filter(act -> act.getPlayerRole() == Role.SPECTATOR).collect(Collectors.toList());
        for (Action act : damage) {
            print("\nSpectator Exploiters");
            print(act.simplify() + " as a spectator");
        }
    }

    private boolean ceaseFire(TreeMap<Long, Boolean> times, long time) {
        List<Long> ts = new ArrayList<>(times.keySet());
        for (int i = 0; i < ts.size(); i++)
            if (time >= ts.get(i) && (i == ts.size() - 1 || time < ts.get(i + 1)))
                return times.get(ts.get(i));
        return false;
    }

    private int secondsToCease(TreeMap<Long, Boolean> times, long time) {
        List<Long> ts = new ArrayList<>(times.keySet());
        for (int i = 0; i < ts.size(); i++)
            if (time >= ts.get(i) && (i == ts.size() - 1 || time < ts.get(i + 1)))
                return times.get(ts.get(i)) ? -1 : (int) (time - ts.get(i));
        return 0;
    }

    private void print(String line) {
        if (lines.contains(line))
            return;
        System.out.println(line);
        lines.add(line);
    }

}
