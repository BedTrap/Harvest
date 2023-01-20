package me.eureka.harvest.systems.modules.miscellaneous.Announcer;

import me.eureka.harvest.events.event.PacketEvent;
import me.eureka.harvest.ic.Setting;
import me.eureka.harvest.systems.modules.Module;
import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import static me.eureka.harvest.systems.modules.miscellaneous.Announcer.AUtils.*;

@Module.Info(name = "Announcer", category = Module.Category.Miscellaneous)
public class Announcer extends Module {
    public Setting<String> join = super.register("Join", List.of("Join", "NameMC"), "Join");
    public Setting<Boolean> leave = super.register("Leave", true);
    public Setting<Boolean> respawn = super.register("Respawn", true);
    public Setting<Boolean> feedback = super.register("Feedback", true);
    public Setting<Boolean> clientSide = super.register("ClientSide", true);

    @Subscribe
    private void onPacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerListS2CPacket packet) {
            switch (packet.getAction()) {
                case ADD_PLAYER -> packet.getEntries().forEach(entry -> {
                    String name = entry.getProfile().getName();
                    if (isBot(name)) return;

                    notify(entry.getProfile().getId().toString(), name);
                });
                case REMOVE_PLAYER -> {
                    if (!leave.get()) return;
                    packet.getEntries().forEach(entry -> {
                        if (!isOnline(entry.getProfile().getId())) return;
                        String name = getNameFromUUID(entry.getProfile().getId());
                        if (isBot(name)) return;

                        if (clientSide.get()) {
                            info(this.name, leaveMessage(name), feedback.get());
                        } else mc.player.sendChatMessage(leaveMessage(name), null);
                    });

                }
                case UPDATE_GAME_MODE -> {
                    if (!respawn.get()) return;
                    packet.getEntries().forEach(entry -> {
                        if (isOnline(entry.getProfile().getId())) {
                            String name = getNameFromUUID(entry.getProfile().getId());
                            if (isBot(name)) return;

                            if (getModeFromUUID(entry.getProfile().getId()).equals(entry.getGameMode())) {
                                if (clientSide.get()) {
                                    info(this.name, name + " respawned.", feedback.get());
                                } else mc.player.sendChatMessage(name + " respawned.", null);
                            }
                        }
                    });
                }
            }
        }
    }

    private void notify(String uuid, String name) {
        if (!clientSide.get()) {
            mc.player.sendChatMessage(joinMessage(name), null);
            return;
        }

        if (join.get("NameMC")) {
            try {
                new Thread(() -> {
                    String content;
                    URLConnection connection;

                    try {
                        connection = new URL("https://api.mojang.com/user/profiles/" + uuid + "/names").openConnection();
                        connection.addRequestProperty("User-Agent", "Mozilla");

                        Scanner scanner = new Scanner(connection.getInputStream());
                        scanner.useDelimiter("\\Z");
                        content = scanner.next();
                        scanner.close();
                    } catch (Exception ignored) {
                        info(this.name, joinMessage(name), feedback.get());
                        return;
                    }

                    String[] m = content.split("\":\"");
                    List<String> names = new ArrayList<>();
                    for (String s : m) {
                        if (s.startsWith("[{")) continue;
                        String[] n = s.split("\"");

                        names.add(n[0]);
                    }

                    names.sort(Comparator.reverseOrder());
                    MutableText text = getSendButton(joinMessage(name), String.join(", ", names));
                    info(this.name, text, feedback.get());
                }).start();
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        } else info(this.name, joinMessage(name), feedback.get());
    }

    private MutableText getSendButton(String message, String hint) {
        MutableText sendButton = Text.literal(message);
        MutableText hintBaseText = Text.literal("");

        MutableText hintMsg = Text.literal(hint);
        hintMsg.setStyle(hintBaseText.getStyle().withFormatting(Formatting.GRAY));
        hintBaseText.append(hintMsg);

        sendButton.setStyle(sendButton.getStyle()
                .withFormatting(Formatting.GRAY)
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        hintBaseText
                )));
        return sendButton;
    }

    private boolean isBot(String name) {
        for (String bot : List.of("xViiRuS", "chpn", "CIT")) {
            if (name.startsWith(bot)) return true;
        }

        return false;
    }
}
