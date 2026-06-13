import {
  Client,
  GatewayIntentBits,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  EmbedBuilder,
  SlashCommandBuilder,
  PermissionFlagsBits,
  MessageFlags,
} from "discord.js";
import {
  NATIONS,
  ROLE_PREFIX,
  roleName,
  pickRandomNation,
} from "./nations.js";
import { ensureAllNations, ensureRole, ensureChannel } from "./guild.js";

const TOKEN = process.env.DISCORD_TOKEN;
const GUILD_ID = process.env.GUILD_ID; // optional: register commands instantly to one guild

if (!TOKEN) {
  console.error("DISCORD_TOKEN is not set. Copy .env.example to .env and fill it in.");
  process.exit(1);
}

const ASSIGN_BUTTON_ID = "nation:assign";

const client = new Client({ intents: [GatewayIntentBits.Guilds] });

// --- slash command definitions ---------------------------------------------

const commands = [
  new SlashCommandBuilder()
    .setName("nation-setup")
    .setDescription("Create the 12 nation roles and their private channels.")
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .setDMPermission(false),
  new SlashCommandBuilder()
    .setName("nation-panel")
    .setDescription("Post the 'Choose your nation' button in this channel.")
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .setDMPermission(false),
].map((c) => c.toJSON());

async function registerCommands() {
  if (GUILD_ID) {
    const guild = await client.guilds.fetch(GUILD_ID);
    await guild.commands.set(commands);
    console.log(`Registered ${commands.length} commands to guild ${GUILD_ID}.`);
  } else {
    await client.application.commands.set(commands);
    console.log(`Registered ${commands.length} global commands (may take ~1h to appear).`);
  }
}

// --- the button + embed -----------------------------------------------------

function buildPanel() {
  const embed = new EmbedBuilder()
    .setTitle("🌍 Choose your nation")
    .setColor(0x4ade80)
    .setDescription(
      "Press the button to be drafted into one of the **12 nations** of the " +
        "Overworld Cup. You'll get a coloured name, your nation's role, and " +
        "access to its private war-room channel.\n\n" +
        NATIONS.map((n) => `• **${n.label}**`).join("\n"),
    )
    .setFooter({ text: "One draw per player — your nation is final." });

  const row = new ActionRowBuilder().addComponents(
    new ButtonBuilder()
      .setCustomId(ASSIGN_BUTTON_ID)
      .setLabel("🎲 Choose my nation")
      .setStyle(ButtonStyle.Success),
  );

  return { embeds: [embed], components: [row] };
}

// --- nation assignment ------------------------------------------------------

// Returns the member's current nation role, if any.
function currentNationRole(member) {
  return member.roles.cache.find((r) => r.name.startsWith(ROLE_PREFIX)) ?? null;
}

async function handleAssign(interaction) {
  await interaction.deferReply({ flags: MessageFlags.Ephemeral });

  const { guild, member } = interaction;

  // One draw per player: if they already belong to a nation, just remind them.
  const existing = currentNationRole(member);
  if (existing) {
    const label = existing.name.slice(ROLE_PREFIX.length);
    const nation = NATIONS.find((n) => n.label === label);
    let channel = null;
    if (nation) {
      const role = await ensureRole(guild, nation);
      channel = await ensureChannel(guild, nation, role);
    }
    await interaction.editReply(
      `You're already representing **${label}**!` +
        (channel ? ` Your war room is <#${channel.id}>.` : ""),
    );
    return;
  }

  const nation = pickRandomNation();
  const role = await ensureRole(guild, nation);
  const channel = await ensureChannel(guild, nation, role);

  await member.roles.add(role, "Drafted into nation via button");

  await interaction.editReply(
    `🎉 You've been drafted into **${nation.label}**!\n` +
      `Your name is now **${nation.label}**-coloured and you can see your ` +
      `private channel: <#${channel.id}>.`,
  );
}

// --- interaction routing ----------------------------------------------------

client.on("interactionCreate", async (interaction) => {
  try {
    if (interaction.isButton() && interaction.customId === ASSIGN_BUTTON_ID) {
      await handleAssign(interaction);
      return;
    }

    if (interaction.isChatInputCommand()) {
      if (interaction.commandName === "nation-setup") {
        await interaction.deferReply({ flags: MessageFlags.Ephemeral });
        await ensureAllNations(interaction.guild);
        await interaction.editReply(
          `✅ Set up ${NATIONS.length} nation roles and private channels.`,
        );
        return;
      }
      if (interaction.commandName === "nation-panel") {
        await interaction.channel.send(buildPanel());
        await interaction.reply({
          content: "✅ Panel posted.",
          flags: MessageFlags.Ephemeral,
        });
        return;
      }
    }
  } catch (err) {
    console.error("Interaction failed:", err);
    const msg = "Something went wrong — make sure the bot's role is above the " +
      "nation roles and has Manage Roles + Manage Channels.";
    if (interaction.deferred || interaction.replied) {
      await interaction.editReply(msg).catch(() => {});
    } else if (interaction.isRepliable()) {
      await interaction.reply({ content: msg, flags: MessageFlags.Ephemeral }).catch(() => {});
    }
  }
});

client.once("ready", async () => {
  console.log(`Logged in as ${client.user.tag}`);
  try {
    await registerCommands();
  } catch (err) {
    console.error("Failed to register commands:", err);
  }
});

client.login(TOKEN);
