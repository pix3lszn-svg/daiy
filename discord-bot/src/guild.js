import {
  ChannelType,
  PermissionFlagsBits,
  OverwriteType,
} from "discord.js";
import {
  NATIONS,
  roleName,
  channelName,
} from "./nations.js";

const CATEGORY_NAME = "🌍 Nations";

// --- roles ------------------------------------------------------------------

// Find an existing role for a nation (matched by name), or create it. The role
// carries the nation's colour, so assigning it changes the member's name colour.
export async function ensureRole(guild, nation) {
  const name = roleName(nation);
  let role = guild.roles.cache.find((r) => r.name === name);
  if (!role) {
    role = await guild.roles.create({
      name,
      color: nation.color,
      mentionable: false,
      reason: `Overworld Cup nation role for ${nation.label}`,
    });
  } else if (role.color !== nation.color) {
    role = await role.setColor(nation.color, "Sync nation colour");
  }
  return role;
}

// All 12 nation roles keyed by nation.key. Creates any that are missing.
export async function ensureAllRoles(guild) {
  const roles = new Map();
  for (const nation of NATIONS) {
    roles.set(nation.key, await ensureRole(guild, nation));
  }
  return roles;
}

// --- channels ---------------------------------------------------------------

async function ensureCategory(guild) {
  let category = guild.channels.cache.find(
    (c) => c.type === ChannelType.GuildCategory && c.name === CATEGORY_NAME,
  );
  if (!category) {
    category = await guild.channels.create({
      name: CATEGORY_NAME,
      type: ChannelType.GuildCategory,
      permissionOverwrites: [
        { id: guild.roles.everyone.id, deny: [PermissionFlagsBits.ViewChannel] },
      ],
    });
  }
  return category;
}

// A private text channel that only the nation's role (and the bot) can see.
// @everyone is denied ViewChannel; the role is granted it — so handing out the
// role is all it takes to let someone into their country's channel.
export async function ensureChannel(guild, nation, role, category) {
  const name = channelName(nation);
  let channel = guild.channels.cache.find(
    (c) => c.type === ChannelType.GuildText && c.name === name,
  );
  const overwrites = [
    {
      id: guild.roles.everyone.id,
      deny: [PermissionFlagsBits.ViewChannel],
      type: OverwriteType.Role,
    },
    {
      id: role.id,
      allow: [
        PermissionFlagsBits.ViewChannel,
        PermissionFlagsBits.SendMessages,
        PermissionFlagsBits.ReadMessageHistory,
      ],
      type: OverwriteType.Role,
    },
  ];
  if (!channel) {
    channel = await guild.channels.create({
      name,
      type: ChannelType.GuildText,
      parent: category?.id,
      topic: `Private war room for ${nation.label}. Only ${nation.label} members can see this.`,
      permissionOverwrites: overwrites,
    });
  } else {
    await channel.permissionOverwrites.set(overwrites);
    if (category && channel.parentId !== category.id) {
      await channel.setParent(category.id, { lockPermissions: false });
    }
  }
  return channel;
}

// Create every role + private channel up front. Returns a map of
// nation.key -> { role, channel } so callers can link to them.
export async function ensureAllNations(guild) {
  const category = await ensureCategory(guild);
  const result = new Map();
  for (const nation of NATIONS) {
    const role = await ensureRole(guild, nation);
    const channel = await ensureChannel(guild, nation, role, category);
    result.set(nation.key, { role, channel });
  }
  return result;
}
