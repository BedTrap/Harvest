package me.eureka.harvest.systems.modules.combat.CevBreakerTest;

import com.google.common.eventbus.Subscribe;
import me.eureka.harvest.events.event.TickEvent;
import me.eureka.harvest.systems.modules.Module;
import me.eureka.harvest.systems.utils.advanced.BlockPosX;
import me.eureka.harvest.systems.utils.anticheat.AntiCheat;
import me.eureka.harvest.systems.utils.anticheat.Range;
import me.eureka.harvest.systems.utils.game.PlayerState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.*;

import static me.eureka.harvest.ic.util.Wrapper.mc;

// My latest module, just trying to rewrite CevBreaker and apply my new code style
@Module.Info(name = "CevBreakerTest", category = Module.Category.Combat)
public class CevBreakerTest {
    private final DataFields field = new DataFields();

    @Subscribe
    public void onTick(TickEvent.Post event) {
        searchTarget();
    }

    private void searchTarget() {
        Map<PlayerEntity, Float> players = new HashMap<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity possible_target) {
                if (!AntiCheat.canKill(possible_target)) return;

                players.put(possible_target, mc.player.distanceTo(possible_target));
            }
        }

        if (players.isEmpty()) {
            field.setTarget(null);
        } else {
            field.setTarget(
                    players.entrySet().stream()
                    .min((a, b) -> Float.compare(a.getValue(), b.getValue()))
                    .get().getKey()
            );
        }
    }

    private void generatePosition() {
        if (field.target == null) return;

        if (PlayerState.isSurrounded(field.target, false)) {

        }
    }

    // Idk really, but this thing makes code more readable
    // Get rid of Class.field, use getters and setters
    // OOP ON TOP
    private static class DataFields {
        private PlayerEntity target;
        private BlockPosX position;

        public void setTarget(PlayerEntity player_entity) {
            target = player_entity;
        }
        public void setPosition(BlockPosX block_pos) {
            position = block_pos;
        }
    }
}

