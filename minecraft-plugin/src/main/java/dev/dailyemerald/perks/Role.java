package dev.dailyemerald.perks;

import java.util.List;
import java.util.Locale;

/** Patreon-backed perk roles. */
public enum Role {
    BOARD("Board Member"),
    JOURNALIST("Journalist");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static Role fromString(String s) {
        if (s == null) return null;
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Picks the best role for a patron's entitled tier titles using the
     * configured name patterns (case-insensitive substring match).
     * BOARD outranks JOURNALIST when a patron somehow has both.
     */
    public static Role resolve(List<String> tierTitles, List<String> boardPatterns, List<String> journalistPatterns) {
        if (matchesAny(tierTitles, boardPatterns)) return BOARD;
        if (matchesAny(tierTitles, journalistPatterns)) return JOURNALIST;
        return null;
    }

    private static boolean matchesAny(List<String> titles, List<String> patterns) {
        for (String title : titles) {
            String lower = title.toLowerCase(Locale.ROOT);
            for (String pattern : patterns) {
                if (lower.contains(pattern.toLowerCase(Locale.ROOT))) return true;
            }
        }
        return false;
    }
}
