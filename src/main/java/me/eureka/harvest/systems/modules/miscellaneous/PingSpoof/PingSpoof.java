package me.eureka.harvest.systems.modules.miscellaneous.PingSpoof;

import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;

@Module.Info(name = "PingSpoof", category = Module.Category.Miscellaneous)
public class PingSpoof extends Module {
    public Setting<Integer> ping = register("Ping", 0, 100, 15000);

    // TODO: 20.01.2023  
}