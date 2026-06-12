import dev.dailyemerald.perks.Role;
import dev.dailyemerald.perks.patreon.PatreonClient;
import dev.dailyemerald.perks.patreon.PatreonMember;

import java.util.List;

/**
 * Plain-Java smoke test for the Patreon JSON parsing and role matching
 * (no JUnit so it can run with nothing but gson on the classpath).
 */
public final class PatreonClientTest {

    private static final String MEMBERS_FIXTURE = """
        {
          "data": [
            {
              "id": "member-1",
              "type": "member",
              "attributes": {"email": "Boss@Example.com", "full_name": "Boss Person", "patron_status": "active_patron"},
              "relationships": {"currently_entitled_tiers": {"data": [{"id": "t1", "type": "tier"}]}}
            },
            {
              "id": "member-2",
              "type": "member",
              "attributes": {"email": "writer@example.com", "full_name": "Writer", "patron_status": "active_patron"},
              "relationships": {"currently_entitled_tiers": {"data": [{"id": "t2", "type": "tier"}]}}
            },
            {
              "id": "member-3",
              "type": "member",
              "attributes": {"email": "gone@example.com", "full_name": "Lapsed", "patron_status": "former_patron"},
              "relationships": {"currently_entitled_tiers": {"data": []}}
            },
            {
              "id": "member-4",
              "type": "member",
              "attributes": {"email": null, "full_name": "Follower", "patron_status": null},
              "relationships": {"currently_entitled_tiers": {"data": []}}
            }
          ],
          "included": [
            {"id": "t1", "type": "tier", "attributes": {"title": "Board Member ($10)"}},
            {"id": "t2", "type": "tier", "attributes": {"title": "Journalist"}}
          ],
          "links": {"next": "https://www.patreon.com/api/oauth2/v2/campaigns/1/members?page=2"},
          "meta": {"pagination": {"total": 4}}
        }
        """;

    private static final String CAMPAIGNS_FIXTURE = """
        {"data": [{"id": "1234567", "type": "campaign", "attributes": {"creation_name": "DailyEmerald"}}]}
        """;

    public static void main(String[] args) {
        PatreonClient.Page page = PatreonClient.parseMembersPage(MEMBERS_FIXTURE);
        check(page.members().size() == 4, "parses all members");
        check(page.nextUrl() != null && page.nextUrl().contains("page=2"), "parses next page link");

        PatreonMember boss = page.members().get(0);
        check("boss@example.com".equals(boss.email()), "lowercases emails");
        check(boss.isActivePatron(), "active patron detected");
        check(boss.tierTitles().equals(List.of("Board Member ($10)")), "tier titles resolved from included[]");

        PatreonMember lapsed = page.members().get(2);
        check(!lapsed.isActivePatron(), "former patron is not active");

        PatreonMember follower = page.members().get(3);
        check(follower.email() == null && !follower.isActivePatron(), "null email/status tolerated");

        List<String> board = List.of("board member");
        List<String> journalist = List.of("journalist");
        check(Role.resolve(boss.tierTitles(), board, journalist) == Role.BOARD,
                "fuzzy-matches 'Board Member ($10)' tier");
        check(Role.resolve(page.members().get(1).tierTitles(), board, journalist) == Role.JOURNALIST,
                "matches Journalist tier");
        check(Role.resolve(List.of("Board Member", "Journalist"), board, journalist) == Role.BOARD,
                "BOARD outranks JOURNALIST");
        check(Role.resolve(List.of("Random Tier"), board, journalist) == null, "unknown tier gives no role");

        check("1234567".equals(PatreonClient.parseFirstCampaignId(CAMPAIGNS_FIXTURE)), "parses campaign id");

        System.out.println("All " + checks + " checks passed.");
    }

    private static int checks = 0;

    private static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            System.err.println("FAILED: " + what);
            System.exit(1);
        }
        System.out.println("ok: " + what);
    }
}
