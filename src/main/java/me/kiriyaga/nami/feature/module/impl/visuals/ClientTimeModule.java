package me.kiriyaga.nami.feature.module.impl.visuals;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PacketReceiveEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import me.kiriyaga.nami.util.container.ShulkerInfo;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.util.ArrayList;
import java.util.List;

import static me.kiriyaga.nami.Nami.MC;

@RegisterModule
public class ClientTimeModule extends Module {

    public final IntSetting value = addSetting(new IntSetting("Time", 25000, 0, 25000));

    public ClientTimeModule() {
        super("ClientTime", "Sets game time client side.", ModuleCategory.of("Render"));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    private void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket)
            event.cancel();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    private void onPreTickEvent(PreTickEvent event) {
        if (MC.world == null || MC.player == null)
            return;

        MC.world.getLevelProperties().setTimeOfDay((long)value.get());
    }
}