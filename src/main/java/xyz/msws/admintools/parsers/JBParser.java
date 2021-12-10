package xyz.msws.admintools.parsers;

import xyz.msws.admintools.Monitor;
import xyz.msws.admintools.data.*;
import xyz.msws.admintools.utils.MSG;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses Jailbreak Logs
 */
public class JBParser extends Parser {

    private final ButtonDatabase buttondb;

    private final List<JailAction> jbActions = new ArrayList<>();
    private final Pattern pattern = Pattern.compile("^\\[\\d\\d:\\d\\d]\s"); // Matches the timecode prefix of jailbreak logs

    public JBParser(Monitor monitor) {
        super(monitor);

        File buttons = new File("buttons.txt");
        this.buttondb = new ButtonDatabase(buttons);
    }

    private final EnumSet<JailActionType> wardenRelated = EnumSet.of(JailActionType.WARDEN, JailActionType.WARDEN_DEATH, JailActionType.PASS, JailActionType.FIRE);

    Set<String> lines = new HashSet<>();

    @Override
    public void parse(String line) {
        if (line.equals("----------------[ JAILBREAK LOGS ]----------------")) {
            jbActions.clear();
            lines.clear();
            return;
        }
        if (line.equals("--------------[ JAILBREAK LOGS END ]--------------")) {
            Arrays.stream(config.getHeader().split("\\\\n")).forEach(System.out::println);

            System.out.println("Jailbreak Logs");
            for (JailAction act : jbActions) {
                if (!config.getActions().contains(act.getType()))
                    continue;
                System.out.println(act.simplify());
            }

            if (config.showEarlyKills())
                checkFreekills();
            if (config.showEarlyVents())
                checkGuardVents();
            if (config.showGameButtons())
                checkWorldButtons();
            if (config.showNades())
                checkNades();
            if (config.showGunPlants())
                checkGuns();
            checkSpectator();
            jbActions.clear();
            lines.clear();
            return;
        }

        if (!line.startsWith("["))
            return;
        if (line.length() < 9)
            return;
        if (line.charAt(8) == '[')
            return;

        if (!pattern.matcher(line).lookingAt())
            return;

        try {
            JailAction jailAction = new JailAction(line);
            jbActions.add(jailAction);
        } catch (IllegalArgumentException e) {
            System.out.println("Unknown jailbreak line: " + line);
        }
    }

    /**
     * Compares warden death / fire / freeday times with CT kills
     */
    private void checkFreekills() {
        List<JailAction> allWarden = jbActions.stream().filter(act -> wardenRelated.contains(act.getType())).sorted().collect(Collectors.toList());
        TreeMap<Long, Boolean> cease = new TreeMap<>();
        for (JailAction act : allWarden) {
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
        for (JailAction act : jbActions) {
            if (act.getPlayerRole().isT())
                ts.add(act.getPlayer());
            if (act.getTargetRole() != null && act.getTargetRole().isT())
                ts.add(act.getTarget());
            if (act.getPlayerRole().isCT())
                cts.add(act.getPlayer());
            if (act.getTargetRole() != null && act.getTargetRole().isCT())
                cts.add(act.getTarget());
        }
        List<JailAction> deaths = jbActions.stream().filter(act -> act.getType() == JailActionType.KILL).collect(Collectors.toList());
        if (!deaths.isEmpty()) {
            boolean ctWin = deaths.get(deaths.size() - 1).getPlayerRole().isCT();
            List<JailAction> ctDeaths = deaths.stream().filter(act -> act.getTargetRole().isCT()).sorted().collect(Collectors.toList());
            List<JailAction> tDeaths = deaths.stream().filter(act -> act.getTargetRole().isT()).sorted().collect(Collectors.toList());
            JailAction lastGuard, lastRequest = null;
            if (tDeaths.size() > 2 && ts.size() - tDeaths.size() <= 2) {
                lastRequest = tDeaths.get(tDeaths.size() - 2);
                cease.put(lastRequest.getTime(), false);
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
            // Last request always after last guard
            if (lastRequest != null)
                System.out.println(lastRequest.getTarget() + " died, activating last request at " + lastRequest.getTimeString());
        }


        List<JailAction> badCombat = jbActions.stream().filter(act -> act.getType() == JailActionType.DAMAGE || act.getType() == JailActionType.KILL)
                .filter(act -> act.getPlayerRole().isCT()).filter(act -> act.getTargetRole() == JailRole.PRISONER)
                .filter(act -> ceaseFire(cease, act.getTime())).collect(Collectors.toList());

        if (!badCombat.isEmpty())
            print("\nGuard Freekills");
        for (JailAction act : badCombat)
            print(act.simplify() + " without warden or " + config.getWardenTimeout() + " " + MSG.plural("second", config.getWardenTimeout()) + " given.");
    }

    /**
     * Checks if a CT broke vents before a T did
     */
    private void checkGuardVents() {
        List<JailAction> breaks = new ArrayList<>();
        for (JailAction act : jbActions) {
            if (act.getType() != JailActionType.VENTS)
                continue;
            if (act.getPlayerRole().isT())
                break;
            breaks.add(act);
        }
        if (!breaks.isEmpty())
            print("\nEarly Guard Vents");
        for (JailAction act : breaks) {
            print(act.simplify() + " before any prisoner did");
        }
    }

    /**
     * Checks if a player pushed a button and another player was damaged by the world soon after
     */
    private void checkWorldButtons() {
        List<JailAction> presses = jbActions.stream().filter(act -> act.getType() == JailActionType.BUTTON).collect(Collectors.toList());
        List<JailAction> damages = jbActions.stream().filter(act -> act.getPlayerRole() == JailRole.WORLD).collect(Collectors.toList());

        List<String> lines = new ArrayList<>();
        for (JailAction press : presses) {
            Button button = buttondb.getButton(press.getOther()[0]);
            if (button.isSafe())
                return;
            StringBuilder result = new StringBuilder();
            List<JailAction> damaged = damages.stream().filter(d -> d.getTime() >= press.getTime() && d.getTime() < press.getTime() + config.getButtonTimeout()).collect(Collectors.toList());
            if (damaged.isEmpty())
                continue;
            result.append(press.simplify());
            result.append(", might've harmed ");
            Set<String> players = new HashSet<>();
            int t = 0, ct = 0;
            for (JailAction act : damaged) {
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

    /**
     * Checks if a player threw a nade and another player was damaged by the world soon after
     */
    private void checkNades() {
        List<JailAction> nades = jbActions.stream().filter(act -> act.getType() == JailActionType.NADE).collect(Collectors.toList());

        for (JailAction act : jbActions.stream().filter(act -> act.getPlayerRole() == JailRole.WORLD).collect(Collectors.toList())) {
            List<JailAction> press = nades.stream()
                    .filter(p -> p.getTime() <= act.getTime() && p.getTime() > act.getTime() - config.getNadeTimeout())
                    .filter(p -> !act.getTarget().equals(p.getPlayer()))
                    .filter(p -> !p.getOther()[0].equals("molotov") && !p.getOther()[0].equals("grenade"))
                    .collect(Collectors.toList());
            for (JailAction p : press) {
                print("\nNade Disruptions");
                print(p.simplify() + " which could've disrupted " + act.getTarget() + " (" + act.getTargetRole().getIcon() + ")");
            }
        }
    }

    /**
     * Checks if a CT drops a gun and a T uses the same type of gun soon after
     */
    private void checkGuns() {

        List<JailAction> guns = jbActions.stream().filter(act -> act.getType() == JailActionType.DROP_WEAPON && act.getPlayerRole().isCT()).collect(Collectors.toList());
        Iterator<JailAction> it = guns.iterator();
        while (it.hasNext()) {
            JailAction drop = it.next();
            JailAction death = jbActions.stream().filter(a -> a.getType() == JailActionType.KILL && a.getTarget().equals(drop.getPlayer())).findFirst().orElse(null);
            if (death == null)
                continue;
            if (death.getTime() == drop.getTime() || death.getTime() + 1 == drop.getTime())
                it.remove();
        }

        for (JailAction act : jbActions.stream().filter(act -> act.getType() == JailActionType.DAMAGE && act.getPlayerRole().isT()).collect(Collectors.toList())) {
            List<JailAction> ds = guns.stream().filter(p -> p.getTime() <= act.getTime() && p.getTime() > act.getTime() - config.getGunTimeout() && p.getOther()[0].equals(act.getOther()[1])).collect(Collectors.toList());
            for (JailAction p : ds) {
                print("\nGun Plants");
                print(p.simplify() + " and " + act.getPlayer() + " (" + act.getPlayerRole().getIcon() + ") used one shortly after");
            }
        }
    }

    /**
     * Checks if a spectator deals damage to anyone
     */
    private void checkSpectator() {
        List<JailAction> damage = jbActions.stream().filter(act -> act.getType() == JailActionType.DAMAGE || act.getType() == JailActionType.KILL)
                .filter(act -> act.getPlayerRole() == JailRole.SPECTATOR).collect(Collectors.toList());
        for (JailAction act : damage) {
            print("\nSpectator Exploiters");
            print(act.simplify() + " as a spectator");
        }
    }

    /**
     * Returns true if CTs should not be shooting prisoners during this time
     *
     * @param times Cease fire map
     * @param time  Time to check
     * @return true if CT should not be shooting
     */
    private boolean ceaseFire(TreeMap<Long, Boolean> times, long time) {
        List<Long> ts = new ArrayList<>(times.keySet());
        for (int i = 0; i < ts.size(); i++)
            if (time > ts.get(i) && (i == ts.size() - 1 || time < ts.get(i + 1)))
                return times.get(ts.get(i));
        return false;
    }

    private void print(String line) {
        if (lines.contains(line))
            return;
        System.out.println(line);
        lines.add(line);
    }

}
