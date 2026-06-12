package dev.dailyemerald.perks.patreon;

import java.util.List;

/** One member of the Patreon campaign, as returned by the v2 API. */
public record PatreonMember(
        String memberId,
        String email,
        String fullName,
        String patronStatus,
        List<String> tierTitles) {

    public boolean isActivePatron() {
        return "active_patron".equals(patronStatus);
    }
}
