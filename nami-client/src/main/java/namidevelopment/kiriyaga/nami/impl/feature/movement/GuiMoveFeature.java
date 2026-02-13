package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.KeyInputEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.nami.impl.feature.visuals.FreecamFeature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.mixin.DuckKeyMapping;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import org.lwjgl.glfw.GLFW;

import static namidevelopment.kiriyaga.api.NamiApi.MC;
import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;

@RegisterFeature
public class GuiMoveFeature extends Feature {

    private boolean forwardHeld = false;
    private boolean backHeld = false;
    private boolean leftHeld = false;
    private boolean rightHeld = false;
    private boolean jumpHeld = false;

    private Screen lastScreen = null;
    private final java.util.Deque<ServerboundContainerClickPacket> clickBuffer = new java.util.ArrayDeque<>();

    public final BoolSetting _2b2t = addSetting(new BoolSetting("2b2t", true));

    public GuiMoveFeature() {
        super("GuiMove", "Allows movement in most GUIs.", FeatureCategory.of("Movement"), "guimove");
    }

    @Override
    public void onDisable() {
        forwardHeld = false;
        backHeld = false;
        leftHeld = false;
        rightHeld = false;
        jumpHeld = false;
        setKeysPressed(false);
        lastScreen = null;
        clickBuffer.clear();
    }

    /*
     Author @cattyngmd
     licensed as nami:
     MIT (2025)
    */
    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent ev){
        if (!_2b2t.get() || MC == null)
            return;

        if (ev.getPacket() instanceof ClientboundContainerClosePacket packet && packet.getContainerId() == MC.player.inventoryMenu.containerId)
            ev.cancel();
    }

//    @SubscribeEvent
//    public void onPacketSend(PacketSendEvent ev) {
//        if (!_2b2t.get()) return;
//
//        if (!(ev.getPacket() instanceof ClickSlotC2SPacket packet)) return;
//
//        if (!isPlayerInv()) {
//            if (!clickBuffer.isEmpty()) clickBuffer.clear();
//            return;
//        }
//
//        if (packet.syncId() != MC.player.playerScreenHandler.syncId) return;
//
//        if (packet.actionType() != SlotActionType.PICKUP) return;
//
//        ev.cancel();
//        clickBuffer.addLast(packet);
//
//        if (clickBuffer.size() > 2) {
//            clickBuffer.clear();
//        }
//    }

//    @SubscribeEvent
//    public void onPreTick(PreTickEvent ev) {
//        if (!_2b2t.get()) return;
//
//        if (!isPlayerInv()) {
//            if (!clickBuffer.isEmpty()) clickBuffer.clear();
//            return;
//        }
//
//        if (clickBuffer.size() < 2) return;
//
//        ClickSlotC2SPacket first = clickBuffer.pollFirst();
//        ClickSlotC2SPacket second = clickBuffer.pollFirst();
//
//        if (first == null || second == null) {
//            clickBuffer.clear();
//            return;
//        }
//
//        MC.getNetworkHandler().sendPacket(first);
//        MC.getNetworkHandler().sendPacket(second);
//        MC.getNetworkHandler().sendPacket(first);
//
//        clickBuffer.clear();
//    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (!canMove()) return;

        updateHeld(MC.options.keyUp, event.key, event.scancode, event.action, event.modifiers, false, v -> forwardHeld = v);
        updateHeld(MC.options.keyDown, event.key, event.scancode, event.action, event.modifiers, false, v -> backHeld = v);
        updateHeld(MC.options.keyLeft, event.key, event.scancode, event.action, event.modifiers, false, v -> leftHeld = v);
        updateHeld(MC.options.keyRight, event.key, event.scancode, event.action, event.modifiers, false, v -> rightHeld = v);
        updateHeld(MC.options.keyJump, event.key, event.scancode, event.action, event.modifiers, false, v -> jumpHeld = v);
    }

    private void updateHeld(KeyMapping bind, int key, int scancode, int action, int modifiers, boolean mouse, java.util.function.Consumer<Boolean> setter) {
        if (!mouse) {
            KeyEvent input = new KeyEvent(key, scancode, modifiers);
            if (!bind.matches(input)) return;
        } else {
            MouseButtonInfo mouseInput = new MouseButtonInfo(key, 0);
            MouseButtonEvent click = new MouseButtonEvent(0, 0, mouseInput);
            if (!bind.matchesMouse(click)) return;
        }

        setter.accept(action == GLFW.GLFW_PRESS);
    }



    @SubscribeEvent
    public void onRender3D(Render3DEvent event) {
        if (FEATURE_SERVICE.getStorage().getByClass(FreecamFeature.class).isEnabled()) return;

        Screen currentScreen = MC.screen;

        if (currentScreen != null) {
            if (lastScreen != currentScreen) {
                resetHeldKeys();
                if (!isPlayerInv()) clickBuffer.clear();
            }
            lastScreen = currentScreen;
        } else {
            lastScreen = null;
            clickBuffer.clear();
        }

        if (!canMove()) {
            setKeysPressed(false);
            return;
        }

        updateKeyWithHold(MC.options.keyUp, forwardHeld);
        updateKeyWithHold(MC.options.keyDown, backHeld);
        updateKeyWithHold(MC.options.keyLeft, leftHeld);
        updateKeyWithHold(MC.options.keyRight, rightHeld);
        updateKeyWithHold(MC.options.keyJump, jumpHeld);
    }

    private void resetHeldKeys() {
        forwardHeld = false;
        backHeld = false;
        leftHeld = false;
        rightHeld = false;
        jumpHeld = false;
        setKeysPressed(false);
    }

    private void updateKeyWithHold(KeyMapping bind, boolean held) {
        InputConstants.Key boundKey = ((DuckKeyMapping) bind).getKey();
        int keyCode = boundKey.getValue();
        boolean physicallyPressed = InputConstants.isKeyDown(MC.getWindow(), keyCode);
        bind.setDown(physicallyPressed || held);
    }

    private boolean canMove() {
        if (MC.screen == null) return true;

        if (MC.screen instanceof ChatScreen
                || MC.screen instanceof SignEditScreen
                || MC.screen instanceof AnvilScreen
                || MC.screen instanceof AbstractCommandBlockEditScreen
                || MC.screen instanceof StructureBlockEditScreen
                || MC.screen instanceof CreativeModeInventoryScreen) {
            return false;
        }

//        if (_2b2t.get() && ( // theese containers doesnt work on 2b, or they do but i dont care
//                MC.currentScreen instanceof ShulkerBoxScreen
//                        || MC.currentScreen instanceof AnvilScreen
//                        || MC.currentScreen instanceof BrewingStandScreen
//                        || MC.currentScreen instanceof CartographyTableScreen
//                        || MC.currentScreen instanceof CrafterScreen
//                        || MC.currentScreen instanceof EnchantmentScreen
//                        || MC.currentScreen instanceof FurnaceScreen
//                        || MC.currentScreen instanceof GrindstoneScreen
//                        || MC.currentScreen instanceof HopperScreen
//                        || MC.currentScreen instanceof HorseScreen
//                        || MC.currentScreen instanceof MerchantScreen
//                        || MC.currentScreen instanceof SmithingScreen
//                        || MC.currentScreen instanceof SmokerScreen
//                        || MC.currentScreen instanceof StonecutterScreen
//                        || MC.currentScreen instanceof GenericContainerScreen
//                        || MC.currentScreen instanceof CreativeInventoryScreen)) {
//            return false;
//        }

        return true;
    }

    private boolean isPlayerInv() {
        if (MC.player == null) return false;
        if (!(MC.screen instanceof InventoryScreen)) return false;
        return MC.player.containerMenu == MC.player.inventoryMenu;
    }

    private void setKeysPressed(boolean pressed) {
        MC.options.keyUp.setDown(pressed);
        MC.options.keyDown.setDown(pressed);
        MC.options.keyLeft.setDown(pressed);
        MC.options.keyRight.setDown(pressed);
        MC.options.keyJump.setDown(pressed);
        MC.options.keyShift.setDown(pressed);
        MC.options.keySprint.setDown(pressed);
    }
}
