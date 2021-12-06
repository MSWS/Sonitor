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

    private final List<JailbreakAction> jailbreakActions = new ArrayList<>();
    private final Pattern pattern = Pattern.compile("^\\[\\d\\d:\\d\\d]\s");

    private final EnumSet<JailActionType> wardenRelated = EnumSet.of(JailActionType.WARDEN, JailActionType.WARDEN_DEATH, JailActionType.PASS, JailActionType.FIRE);

    Set<String> lines = new HashSet<>();

    @Override
    public void parse(String line) {
        if (line.equals("----------------[ JAILBREAK LOGS ]----------------")) {
            jailbreakActions.clear();
            return;
        }
        if (line.equals("--------------[ JAILBREAK LOGS END ]--------------")) {
            Arrays.stream(config.getHeader().split("\\\\n")).forEach(System.out::println);

            System.out.println("Jailbreak Logs");
            for (JailbreakAction act : jailbreakActions) {
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
            jailbreakActions.clear();
            lines.clear();
            return;
        }


        if (!line.startsWith("["))
            return;

        if (!pattern.matcher(line).lookingAt())
            return;

        JailbreakAction jailbreakAction = new JailbreakAction(line);
        jailbreakActions.add(jailbreakAction);
    }

    private void checkGuardTime() {
        List<JailbreakAction> allWarden = jailbreakActions.stream().filter(act -> wardenRelated.contains(act.getType())).sorted().collect(Collectors.toList());
        TreeMap<Long, Boolean> cease = new TreeMap<>();

        for (JailbreakAction act : allWarden) {
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
        for (JailbreakAction act : jailbreakActions) {
            if (act.getPlayerRole().isT())
                ts.add(act.getPlayer());
            if (act.getTargetRole() != null && act.getTargetRole().isT())
                ts.add(act.getTarget());
            if (act.getPlayerRole().isCT())
                cts.add(act.getPlayer());
            if (act.getTargetRole() != null && act.getTargetRole().isCT())
                cts.add(act.getTarget());
        }
        List<JailbreakAction> deaths = jailbreakActions.stream().filter(act -> act.getType() == JailActionType.KILL).collect(Collectors.toList());
        if (!deaths.isEmpty()) {
            boolean ctWin = deaths.get(deaths.size() - 1).getPlayerRole().isCT();
            List<JailbreakAction> ctDeaths = deaths.stream().filter(act -> act.getTargetRole().isCT()).sorted().collect(Collectors.toList());
            List<JailbreakAction> tDeaths = deaths.stream().filter(act -> act.getTargetRole().isT()).sorted().collect(Collectors.toList());
            JailbreakAction lastGuard, lastRequest = null;
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


        List<JailbreakAction> badCombat = jailbreakActions.stream().filter(act -> act.getType() == JailActionType.DAMAGE || act.getType() == JailActionType.KILL)
                .filter(act -> act.getPlayerRole() == Role.GUARD || act.getPlayerRole() == Role.WARDEN).filter(act -> act.getTargetRole() == Role.PRISONER)
                .filter(act -> ceaseFire(cease, act.getTime())).collect(Collectors.toList());

        if (!badCombat.isEmpty())
            print("\nGuard Freekills");
        for (JailbreakAction act : badCombat) {
            int seconds = secondsToCease(cease, act.getTime());
            if (seconds == -1) {
                print(act.simplify() + " with no warden.");
                continue;
            }
            print(act.simplify() + " within " + seconds + MSG.plural("second", seconds) + " of new warden");
        }
    }

    private void checkGuardVents() {
        List<JailbreakAction> breaks = new ArrayList<>();
        for (JailbreakAction act : jailbreakActions) {
            if (act.getType() != JailActionType.VENTS)
                continue;
            if (act.getPlayerRole() == Role.PRISONER)
                break;
            breaks.add(act);
        }
        if (!breaks.isEmpty())
            print("\nEarly Guard Vents");
        for (JailbreakAction act : breaks) {
            print(act.simplify() + " before any prisoner did");
        }
    }

    private void checkWorldButtons() {
        List<JailbreakAction> presses = jailbreakActions.stream().filter(act -> act.getType() == JailActionType.BUTTON).collect(Collectors.toList());
        List<JailbreakAction> damages = jailbreakActions.stream().filter(act -> act.getPlayerRole() == Role.WORLD).collect(Collectors.toList());

        List<String> lines = new ArrayList<>();
        for (JailbreakAction press : presses) {
            Button button = buttondb.getButton(press.getOther()[0]);
            if (button.isSafe())
                return;
            StringBuilder result = new StringBuilder();
            List<JailbreakAction> damaged = damages.stream().filter(d -> d.getTime() >= press.getTime() && d.getTime() < press.getTime() + config.getButtonTimeout()).collect(Collectors.toList());
            if (damaged.isEmpty())
                continue;
            result.append(press.simplify());
            result.append(" which could've harmed ");
            Set<String> players = new HashSet<>();
            int t = 0, ct = 0;
            for (JailbreakAction act : damaged) {
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
        List<JailbreakAction> nades = jailbreakActions.stream().filter(act -> act.getType() == JailActionType.NADE).collect(Collectors.toList());

        for (JailbreakAction act : jailbreakActions.stream().filter(act -> act.getPlayerRole() == Role.WORLD).collect(Collectors.toList())) {
            List<JailbreakAction> press = nades.stream().filter(p -> p.getTime() <= act.getTime() && p.getTime() > act.getTime() - config.getNadeTimeout())
                    .filter(p -> !act.getTarget().equals(p.getPlayer())).collect(Collectors.toList());
            for (JailbreakAction p : press) {
                print("\nNade Disruptions");
                print(p.simplify() + " which could've disrupted " + act.getTarget() + " (" + act.getTargetRole().getIcon() + ")");
            }
        }
    }

    private void checkGuns() {
        Map<String, List<JailbreakAction>> drops = new HashMap<>();

        List<JailbreakAction> guns = jailbreakActions.stream().filter(act -> act.getType() == JailActionType.DROP_WEAPON).collect(Collectors.toList());
        for (JailbreakAction act : guns) {
            String gun = act.getOther()[0];
            List<JailbreakAction> related = drops.getOrDefault(gun, new ArrayList<>());
            related.add(act);
            drops.put(gun, related);
        }

        for (JailbreakAction act : jailbreakActions.stream().filter(act -> act.getType() == JailActionType.DAMAGE && act.getPlayerRole().isT()).collect(Collectors.toList())) {
            List<JailbreakAction> ds = guns.stream().filter(p -> p.getTime() <= act.getTime() && p.getTime() > act.getTime() - config.getGunTimeout() && p.getOther()[0].equals(act.getOther()[1])).collect(Collectors.toList());
            for (JailbreakAction p : ds) {
                print("\nGun Plants");
                print(p.simplify() + " and " + act.getPlayer() + " (" + act.getPlayerRole().getIcon() + ") used one shortly after");
            }
        }
    }

    private void checkSpectator() {
        List<JailbreakAction> damage = jailbreakActions.stream().filter(act -> act.getType() == JailActionType.DAMAGE || act.getType() == JailActionType.KILL)
                .filter(act -> act.getPlayerRole() == Role.SPECTATOR).collect(Collectors.toList());
        for (JailbreakAction act : damage) {
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
