package justfatlard.village_quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Fires when somebody finishes something a villager asked of them.
 *
 * <p>A quest has no item to hold up at the end - the reward is reputation and a different
 * conversation next time - so there is nothing an inventory trigger could watch for. This is the
 * moment itself, reported from the one place every kind of quest passes through on its way to
 * being done.
 */
public class QuestCompleteCriterion extends SimpleCriterionTrigger<QuestCompleteCriterion.Conditions> {

	@Override
	public Codec<Conditions> codec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayer player) {
		this.trigger(player, conditions -> true);
	}

	public record Conditions(Optional<Holder<LootItemCondition>> player)
			implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Conditions::player)
			).apply(instance, Conditions::new)
		);
	}
}
