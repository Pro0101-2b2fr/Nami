package namidevelopment.kiriyaga.nami.impl.feature.hud;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterFeature
public class PacketsFeature extends HudElementFeature {

    public enum LayoutMode { HORIZONTAL, VERTICAL}

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));
    public final EnumSetting<LayoutMode> layout = addSetting(new EnumSetting<>("Layout", LayoutMode.HORIZONTAL));

    private final List<MsPacket> packets = new ArrayList<>();

    public PacketsFeature() {
        super("Packets", "Displays sended packets.", 0, 0, 100, 30);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPacket(PacketReceiveEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet == null) return;
        long now = System.currentTimeMillis();
        MC.execute(() -> {
            packets.add(new MsPacket(packet, now));
            cleanup(now);
        });
    }

    private void cleanup(long now) {
        long cutoff = now - 1000L;
        Iterator<MsPacket> it = packets.iterator();
        while (it.hasNext()) {
            if (it.next().time < cutoff) {
                it.remove();
            }
        }
    }

    private int countGlobal() {
        return packets.size();
    }

    private int countSwaps() {
        return (int) packets.stream().filter(p -> p.packet instanceof ServerboundSetCarriedItemPacket).count();
    }

    private int countAction() {
        return (int) packets.stream().filter(p -> p.packet instanceof ServerboundPlayerActionPacket).count();
    }

    private int countInteractEntity() {
        return (int) packets.stream().filter(p -> p.packet instanceof ServerboundInteractPacket).count();
    }

    private int countInteractBlock() {
        return (int) packets.stream().filter(p -> p.packet.getClass().getSimpleName().toLowerCase().contains("useitemon")).count();
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayText() {
        if (layout.get() == LayoutMode.VERTICAL) return null;

        long now = System.currentTimeMillis();
        cleanup(now);
        int swaps = countSwaps();
        int global = countGlobal();
        int action = countAction();
        int interact = countInteractEntity();
        int block = countInteractBlock();

        String text = "";

        if (displayLabel.get()) {
            text += "{global}Packets ";
        }

        text += "{global}({white}S:" + swaps + "{global}, {white}G:" + global + "{global}, {white}A:" + action + "{global}, {white}I:" + interact + "{global}, {white}B:" + block + "{global})";
        width = FONT_SERVICE.getWidth(text.replaceAll("\\{.*?}", ""));
        height = FONT_SERVICE.getHeight();

        return CAT_FORMAT.format(text);
    }

    @Override
    public List<TextElement> getTextElements() {
        if (layout.get() != LayoutMode.VERTICAL) {
            return super.getTextElements();
        }
        long now = System.currentTimeMillis();
        cleanup(now);
        int swaps = countSwaps();
        int global = countGlobal();
        int action = countAction();
        int interact = countInteractEntity();
        int block = countInteractBlock();

        List<TextElement> lines = new ArrayList<>();
        int lineHeight = FONT_SERVICE.getHeight() + 1;
        int offsetY = 0;

        lines.add(new TextElement(CAT_FORMAT.format("{global}Swaps: {white}" + swaps), 0, offsetY));
        offsetY += lineHeight;

        lines.add(new TextElement(CAT_FORMAT.format("{global}Global: {white}" + global), 0, offsetY));
        offsetY += lineHeight;

        lines.add(new TextElement(CAT_FORMAT.format("{global}Action: {white}" + action), 0, offsetY));
        offsetY += lineHeight;

        lines.add(new TextElement(CAT_FORMAT.format("{global}Interact: {white}" + interact), 0, offsetY));
        offsetY += lineHeight;

        lines.add(new TextElement(CAT_FORMAT.format("{global}Block: {white}" + block), 0, offsetY));
        offsetY += lineHeight;

        int maxWidth = lines.stream().mapToInt(te -> FONT_SERVICE.getWidth(te.text().getString().replaceAll("\\{.*?}", ""))).max().orElse(0);
        width = maxWidth;
        height = offsetY;

        return lines;
    }

    private static class MsPacket {
        public final Packet<?> packet;
        public final long time;
        public MsPacket(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }
}
