package justfatlard.village_quests;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillageQuestsConfig {
   private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");
   private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("village-quests.properties");
   private static long interactionCooldownMs = 500L;
   private static long chestPenaltyCooldownMs = 60000L;
   private static long blockPlacementCooldownMs = 300000L;
   private static float misnomerChance = 0.03F;
   private static float deepQuestChance = 0.05F;
   private static int deepQuestCooldownDays = 3;
   private static float gatheringChance = 0.15F;
   private static int gatheringMinDays = 3;
   private static int gatheringMaxDays = 7;
   private static int reputationDecayThreshold = 100;
   private static float reputationDecayRate = 0.01F;
   private static int questReminderIntervalTicks = 1200;
   private static int presenceTrackingIntervalTicks = 200;
   private static int bossBarUpdateIntervalTicks = 20;
   private static int mailPerPlayerPerDay = 2;
   private static int mailBasePerVillage = 5;
   private static final String DEFAULT_CONFIG = "# Village Quests Configuration\n# Delete this file to regenerate with defaults.\n\n# ---- Interaction Cooldowns ----\n\n# Milliseconds between dialogue interactions with the same villager.\n# Prevents duplicate dialogue packets from rapid clicking.\ninteraction_cooldown_ms=500\n\n# Milliseconds before opening the same chest triggers another reputation penalty.\nchest_penalty_cooldown_ms=60000\n\n# Milliseconds before placing the same type of block (bed, job block, emerald block)\n# grants another reputation bonus. Prevents place/break exploit loops.\nblock_placement_cooldown_ms=300000\n\n# ---- Quest Rarity ----\n\n# Base chance (0.0 to 1.0) for a misnomer quest to appear per interaction.\n# Doubled when the villager and player are alone.\nmisnomer_chance=0.03\n\n# Chance (0.0 to 1.0) for a deep quest to be offered when eligible.\ndeep_quest_chance=0.05\n\n# Minimum real-time days between deep quests for the same villager.\ndeep_quest_cooldown_days=3\n\n# ---- Gatherings ----\n\n# Chance (0.0 to 1.0) that a gathering starts on an eligible dawn.\ngathering_chance=0.15\n\n# Minimum Minecraft days between gatherings in the same village.\ngathering_min_days=3\n\n# Maximum Minecraft days between gatherings in the same village.\ngathering_max_days=7\n\n# ---- Reputation ----\n\n# Reputation above this value decays each Minecraft day.\n# Keeps high trust impermanent without continued presence.\nreputation_decay_threshold=100\n\n# Fraction (0.0 to 1.0) of reputation above the threshold that decays per day.\n# At 200 reputation with 0.01 rate: loses 1 point per day.\nreputation_decay_rate=0.01\n\n# ---- Tick Intervals ----\n\n# Ticks between quest objective reminders on the action bar (20 ticks = 1 second).\nquest_reminder_interval_ticks=1200\n\n# Ticks between presence tracking checks.\npresence_tracking_interval_ticks=200\n\n# Ticks between boss bar updates.\nboss_bar_update_interval_ticks=20\n\n# ---- Mail ----\n\n# Maximum mail a single player can receive per Minecraft day per village.\nmail_per_player_per_day=2\n\n# Base village-wide mail cap per day (scales up with active players).\nmail_base_per_village=5\n";

   public static void load() {
      Path path = CONFIG_PATH;
      if (!Files.exists(path)) {
         createDefaultConfig(path);
         LOGGER.info("[{}] Created default config at {}", "village-quests-justfatlard", CONFIG_PATH);
      } else {
         Properties props = new Properties();

         try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
         } catch (IOException var7) {
            LOGGER.error("[{}] Failed to read config file, using defaults: {}", "village-quests-justfatlard", var7.getMessage());
            return;
         }

         interactionCooldownMs = getLong(props, "interaction_cooldown_ms", interactionCooldownMs);
         chestPenaltyCooldownMs = getLong(props, "chest_penalty_cooldown_ms", chestPenaltyCooldownMs);
         blockPlacementCooldownMs = getLong(props, "block_placement_cooldown_ms", blockPlacementCooldownMs);
         misnomerChance = getFloat(props, "misnomer_chance", misnomerChance);
         deepQuestChance = getFloat(props, "deep_quest_chance", deepQuestChance);
         deepQuestCooldownDays = getInt(props, "deep_quest_cooldown_days", deepQuestCooldownDays);
         gatheringChance = getFloat(props, "gathering_chance", gatheringChance);
         gatheringMinDays = getInt(props, "gathering_min_days", gatheringMinDays);
         gatheringMaxDays = getInt(props, "gathering_max_days", gatheringMaxDays);
         reputationDecayThreshold = getInt(props, "reputation_decay_threshold", reputationDecayThreshold);
         reputationDecayRate = getFloat(props, "reputation_decay_rate", reputationDecayRate);
         questReminderIntervalTicks = getInt(props, "quest_reminder_interval_ticks", questReminderIntervalTicks);
         presenceTrackingIntervalTicks = getInt(props, "presence_tracking_interval_ticks", presenceTrackingIntervalTicks);
         bossBarUpdateIntervalTicks = getInt(props, "boss_bar_update_interval_ticks", bossBarUpdateIntervalTicks);
         mailPerPlayerPerDay = getInt(props, "mail_per_player_per_day", mailPerPlayerPerDay);
         mailBasePerVillage = getInt(props, "mail_base_per_village", mailBasePerVillage);
         LOGGER.info("[{}] Config loaded from {}", "village-quests-justfatlard", CONFIG_PATH);
      }
   }

   public static long getInteractionCooldownMs() {
      return interactionCooldownMs;
   }

   public static long getChestPenaltyCooldownMs() {
      return chestPenaltyCooldownMs;
   }

   public static long getBlockPlacementCooldownMs() {
      return blockPlacementCooldownMs;
   }

   public static float getMisnomerChance() {
      return misnomerChance;
   }

   public static float getDeepQuestChance() {
      return deepQuestChance;
   }

   public static int getDeepQuestCooldownDays() {
      return deepQuestCooldownDays;
   }

   public static float getGatheringChance() {
      return gatheringChance;
   }

   public static int getGatheringMinDays() {
      return gatheringMinDays;
   }

   public static int getGatheringMaxDays() {
      return gatheringMaxDays;
   }

   public static int getReputationDecayThreshold() {
      return reputationDecayThreshold;
   }

   public static float getReputationDecayRate() {
      return reputationDecayRate;
   }

   public static int getQuestReminderIntervalTicks() {
      return questReminderIntervalTicks;
   }

   public static int getPresenceTrackingIntervalTicks() {
      return presenceTrackingIntervalTicks;
   }

   public static int getBossBarUpdateIntervalTicks() {
      return bossBarUpdateIntervalTicks;
   }

   public static int getMailPerPlayerPerDay() {
      return mailPerPlayerPerDay;
   }

   public static int getMailBasePerVillage() {
      return mailBasePerVillage;
   }

   public static long getDeepQuestCooldownMs() {
      return deepQuestCooldownDays * 24L * 60L * 60L * 1000L;
   }

   public static long getGatheringMinTicks() {
      return gatheringMinDays * 24000L;
   }

   public static long getGatheringMaxTicks() {
      return gatheringMaxDays * 24000L;
   }

   private static long getLong(Properties props, String key, long defaultValue) {
      String value = props.getProperty(key);
      if (value != null && !value.isBlank()) {
         try {
            return Long.parseLong(value.trim());
         } catch (NumberFormatException var6) {
            LOGGER.warn("[{}] Invalid value for '{}': '{}', using default {}", new Object[]{"village-quests-justfatlard", key, value, defaultValue});
            return defaultValue;
         }
      } else {
         return defaultValue;
      }
   }

   private static int getInt(Properties props, String key, int defaultValue) {
      String value = props.getProperty(key);
      if (value != null && !value.isBlank()) {
         try {
            return Integer.parseInt(value.trim());
         } catch (NumberFormatException var5) {
            LOGGER.warn("[{}] Invalid value for '{}': '{}', using default {}", new Object[]{"village-quests-justfatlard", key, value, defaultValue});
            return defaultValue;
         }
      } else {
         return defaultValue;
      }
   }

   private static float getFloat(Properties props, String key, float defaultValue) {
      String value = props.getProperty(key);
      if (value != null && !value.isBlank()) {
         try {
            return Float.parseFloat(value.trim());
         } catch (NumberFormatException var5) {
            LOGGER.warn("[{}] Invalid value for '{}': '{}', using default {}", new Object[]{"village-quests-justfatlard", key, value, defaultValue});
            return defaultValue;
         }
      } else {
         return defaultValue;
      }
   }

   private static void createDefaultConfig(Path path) {
      try {
         Files.createDirectories(path.getParent());
         Files.writeString(
            path,
            "# Village Quests Configuration\n# Delete this file to regenerate with defaults.\n\n# ---- Interaction Cooldowns ----\n\n# Milliseconds between dialogue interactions with the same villager.\n# Prevents duplicate dialogue packets from rapid clicking.\ninteraction_cooldown_ms=500\n\n# Milliseconds before opening the same chest triggers another reputation penalty.\nchest_penalty_cooldown_ms=60000\n\n# Milliseconds before placing the same type of block (bed, job block, emerald block)\n# grants another reputation bonus. Prevents place/break exploit loops.\nblock_placement_cooldown_ms=300000\n\n# ---- Quest Rarity ----\n\n# Base chance (0.0 to 1.0) for a misnomer quest to appear per interaction.\n# Doubled when the villager and player are alone.\nmisnomer_chance=0.03\n\n# Chance (0.0 to 1.0) for a deep quest to be offered when eligible.\ndeep_quest_chance=0.05\n\n# Minimum real-time days between deep quests for the same villager.\ndeep_quest_cooldown_days=3\n\n# ---- Gatherings ----\n\n# Chance (0.0 to 1.0) that a gathering starts on an eligible dawn.\ngathering_chance=0.15\n\n# Minimum Minecraft days between gatherings in the same village.\ngathering_min_days=3\n\n# Maximum Minecraft days between gatherings in the same village.\ngathering_max_days=7\n\n# ---- Reputation ----\n\n# Reputation above this value decays each Minecraft day.\n# Keeps high trust impermanent without continued presence.\nreputation_decay_threshold=100\n\n# Fraction (0.0 to 1.0) of reputation above the threshold that decays per day.\n# At 200 reputation with 0.01 rate: loses 1 point per day.\nreputation_decay_rate=0.01\n\n# ---- Tick Intervals ----\n\n# Ticks between quest objective reminders on the action bar (20 ticks = 1 second).\nquest_reminder_interval_ticks=1200\n\n# Ticks between presence tracking checks.\npresence_tracking_interval_ticks=200\n\n# Ticks between boss bar updates.\nboss_bar_update_interval_ticks=20\n\n# ---- Mail ----\n\n# Maximum mail a single player can receive per Minecraft day per village.\nmail_per_player_per_day=2\n\n# Base village-wide mail cap per day (scales up with active players).\nmail_base_per_village=5\n"
         );
      } catch (IOException var2) {
         LOGGER.error("[{}] Failed to create default config: {}", "village-quests-justfatlard", var2.getMessage());
      }
   }
}
