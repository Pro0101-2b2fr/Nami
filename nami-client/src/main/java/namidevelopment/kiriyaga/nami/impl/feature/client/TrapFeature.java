package namidevelopment.kiriyaga.nami.impl.feature.client;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.Timer;

@RegisterFeature
public class TrapFeature extends Feature {

    public enum Mode {TICKS, MS}

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Timing", Mode.TICKS));
    public final IntSetting delayTick = addSetting(new IntSetting("DelayTick", "Delay", 0, 0, 20));
    public final DoubleSetting delayMilliseconds = addSetting(new DoubleSetting("DelayMS", "Delay", 0.00, 0.00, 3000.00));
    public final IntSetting shiftTicks = addSetting(new IntSetting("ShiftTicks", 3, 1, 30));

    public final Timer timer = new Timer();
    public int tickCD = 0;
    public boolean window = false;
    public int windowPlaced = 0;
    public TrapFeature() {
        super("Trap", "Global configurations for trap features.", FeatureCategory.of("Client"));

        if (!this.isEnabled())
            this.toggle();

        delayMilliseconds.setShowCondition(() -> mode.get() == Mode.MS);
        delayTick.setShowCondition(() -> mode.get() == Mode.TICKS);
    }

    @Override
    public void onDisable() {
        if (!this.isEnabled())
            this.toggle();
    }

    public void reset() {
        tickCD = 0;
        window = false;
        windowPlaced = 0;
        timer.reset();
    }
}
