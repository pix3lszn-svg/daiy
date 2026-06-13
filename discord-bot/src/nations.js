// The 12 nations of the Overworld Cup. Colours match the Minecraft plugin's
// per-nation jersey colours (see minecraft-plugin .../nations/NationManager.java)
// so a player's Discord name colour lines up with their in-game team.
//
// `key`   — lowercase id, used for role/channel names and lookups.
// `label` — display name shown to players.
// `color` — RGB integer used for the Discord role colour (and name colour).
export const NATIONS = [
  { key: "darkforest", label: "Dark Forest", color: 4857690 },
  { key: "desert", label: "Desert", color: 14711609 },
  { key: "forest", label: "Forest", color: 2976335 },
  { key: "jungle", label: "Jungle", color: 5754884 },
  { key: "mesa", label: "Mesa", color: 12597547 },
  { key: "mushroom", label: "Mushroom", color: 12720219 },
  { key: "plains", label: "Plains", color: 16105752 },
  { key: "savanna", label: "Savanna", color: 13930522 },
  { key: "snow", label: "Snow", color: 11067626 },
  { key: "taiga", label: "Taiga", color: 3825546 },
  { key: "cherry", label: "Cherry", color: 16027569 },
  { key: "swamp", label: "Swamp", color: 3051386 },
];

const BY_KEY = new Map(NATIONS.map((n) => [n.key, n]));

export function nationByKey(key) {
  return BY_KEY.get(key);
}

// Role/channel naming. The role is the source of truth for "which nation are
// you" (it both colours the name and unlocks the private channel).
export const ROLE_PREFIX = "Nation: ";

export function roleName(nation) {
  return `${ROLE_PREFIX}${nation.label}`;
}

export function channelName(nation) {
  return `${nation.key}-nation`;
}

export function pickRandomNation() {
  return NATIONS[Math.floor(Math.random() * NATIONS.length)];
}
