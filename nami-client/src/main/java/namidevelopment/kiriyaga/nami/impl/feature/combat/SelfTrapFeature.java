package namidevelopment.kiriyaga.nami.impl.feature.combat;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.nami.impl.feature.combat.component.TrapComponent;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static namidevelopment.kiriyaga.api.NamiApi.MC;
import static namidevelopment.kiriyaga.api.util.BlockUtils.getSurround;
import static namidevelopment.kiriyaga.api.util.BlockUtils.isPlaceable;

@RegisterFeature
public class SelfTrapFeature extends Feature {

    public final BoolSetting face = addSetting(new BoolSetting("Face", true));
    public final BoolSetting head = addSetting(new BoolSetting("Head", true));
    public final BoolSetting extension = addSetting(new BoolSetting("Extension", false));
    public final BoolSetting jumpDisable = addSetting(new BoolSetting("JumpDisable", false));
    public final BoolSetting selfToggle = addSetting(new BoolSetting("SelfToggle", false));

    private final TrapComponent trap;

    public SelfTrapFeature() {
        super("SelfTrap", "Traps you to prevent damage.", FeatureCategory.of("Combat"), "selftrap");
        this.trap = new TrapComponent(this);
    }

    @Override
    public void onDisable() {
        trap.onDisable();
    }

    @SubscribeEvent
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;

        this.clearDisplayInfo();

        if (jumpDisable.get() && !MC.player.onGround()) {
            this.toggle();
            return;
        }

        List<BlockPos> targets = getTrapTargets();
        this.addDisplayInfo(targets.size() + "");
        if (targets.isEmpty() && selfToggle.get()) {
            this.toggle();
            return;
        }

        trap.onTick(event, this, targets);
    }

    @SubscribeEvent
    public void onRender(Render3DEvent event) {
        trap.onRender(event);
    }

    private List<BlockPos> getTrapTargets() {
        int baseHeight = (face.get() && !MC.player.isVisuallyCrawling()) ? 1 : 0;

        List<BlockPos> targets = new ArrayList<>(getSurround(MC.player, baseHeight, extension.get()));
        if (head.get()) {
            targets.addAll(getSurround(MC.player, baseHeight + 1, extension.get()));
            BlockPos b = MC.player.getOnPos().above().above(baseHeight*2);

            if (!isPlaceable(b))
                targets.add(b);
        }

        return targets;
    }
}
