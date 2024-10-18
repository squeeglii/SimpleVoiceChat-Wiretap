package de.maxhenkel.wiretap.mixin;

import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.utils.HeadUtils;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.UUID;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow
    @Nullable
    private String itemName;

    @Unique
    private UUID currentResultId;
    @Unique
    private ItemStack currentInputItem;

    public AnvilMenuMixin(@Nullable MenuType<?> menuType, int i, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(menuType, i, inventory, containerLevelAccess);
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V", ordinal = 4, shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void createResult(CallbackInfo ci, ItemStack itemStack, int i, long l, int j, ItemStack itemStack2) {
        if (!Wiretap.SERVER_CONFIG.anvilCrafting.get()) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (!itemStack.getItem().equals(Items.CALIBRATED_SCULK_SENSOR)) {
            return;
        }

        if (itemName == null || !itemName.equalsIgnoreCase("wiretap")) {
            return;
        }

        currentResultId = UUID.randomUUID();
        ItemStack microphone = HeadUtils.createMicrophone(currentResultId);
        resultSlots.setItem(0, microphone);
        currentInputItem = itemStack.copy();
    }

    @Inject(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V", ordinal = 0, shift = At.Shift.AFTER))
    public void onTake(Player player, ItemStack result, CallbackInfo ci) {
        if (!Wiretap.SERVER_CONFIG.anvilCrafting.get()) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (currentResultId == null || currentInputItem == null) {
            return;
        }
        ItemStack inputItem = currentInputItem.copy();
        inputItem.setCount(currentInputItem.getCount() - 1);
        if (inputItem.getCount() <= 0) {
            inputItem = ItemStack.EMPTY;
        }
        ItemStack speaker = HeadUtils.createSpeaker(currentResultId);
        currentResultId = null;
        currentInputItem = null;
        // setItem calls createResult again
        inputSlots.setItem(0, inputItem);

        boolean added = player.getInventory().add(speaker);
        if (!added || !speaker.isEmpty()) {
            player.drop(speaker, false);
        }
    }

}
