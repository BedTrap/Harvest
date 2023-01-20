package me.eureka.harvest.events.event;

import me.eureka.harvest.events.Cancelled;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;

public class MoveEvent {
    public static class Player extends Cancelled {
        public MovementType type;
        public Vec3d movement;

        public Player(MovementType type, Vec3d movement) {
            this.type = type;
            this.movement = movement;
        }
    }

    public static class Elytra extends Cancelled {
        public Vec3d movement;

        public Elytra(Vec3d movement) {
            this.movement = movement;
        }
    }

    public static class Boat extends Cancelled {
        public BoatEntity boat;

        public Boat(BoatEntity boat) {
            this.boat = boat;
        }
    }

    public static class Entity extends Cancelled {
        public Vec3d movement;

        public Entity(Vec3d movement) {
            this.movement = movement;
        }
    }
}
