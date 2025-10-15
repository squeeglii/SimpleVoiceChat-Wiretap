package de.maxhenkel.wiretap.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.utils.HeadUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
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

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V", ordinal = 4, shift = At.Shift.AFTER))
    public void createResult(CallbackInfo ci, @Local(ordinal = 0) ItemStack itemStack) {
        if (!Wiretap.SERVER_CONFIG.anvilCrafting.get()) {
            return;
        }

        if (this.player.level().isClientSide()) {
            return;
        }

        if (!itemStack.getItem().equals(Items.CALIBRATED_SCULK_SENSOR)) {
            return;
        }

        if (itemName == null || !itemName.equalsIgnoreCase("wiretap")) {
            return;
        }

        this.currentResultId = UUID.randomUUID();
        Optional<ItemStack> optMicrophone = HeadUtils.createMicrophone(this.currentResultId);
        Optional<ItemStack> optSpeaker = HeadUtils.createSpeaker(this.currentResultId);

        if(optMicrophone.isEmpty()) {
            this.currentResultId = null;
            Wiretap.LOGGER.error("Could not create microphone item from anvil recipe", new Throwable());
            return;
        }

        if(optSpeaker.isEmpty()) {
            this.currentResultId = null;
            Wiretap.LOGGER.error("Could not create speaker item from anvil recipe", new Throwable());
            return;
        }

        ItemStack holder = new ItemStack(Items.BUNDLE, itemStack.getCount());
        holder.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(
                optSpeaker.get(),
                optMicrophone.get()
        )));

        this.resultSlots.setItem(0, holder);
        this.currentInputItem = itemStack.copy();
    }

    // PuzzleLib compat - using HEAD instead. Any combination of return or switching to a different opcode got
    // swallowed up.
    //@Inject(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V", ordinal = 0, shift = At.Shift.AFTER))
    //@Inject(method = "onTake", at = @At(value = "HEAD"))
    @Unique
    public void onTake(Player player, ItemStack result, CallbackInfo ci) {
        if (!Wiretap.SERVER_CONFIG.anvilCrafting.get()) return;
        if (player.level().isClientSide()) return;
        if (currentResultId == null || currentInputItem == null) return;


        ItemStack inputItem = currentInputItem.copy();
        inputItem.setCount(currentInputItem.getCount() - 1);
        if (inputItem.getCount() <= 0) {
            inputItem = ItemStack.EMPTY;
        }

        Optional<ItemStack> optSpeaker = HeadUtils.createSpeaker(this.currentResultId);

        currentResultId = null;
        currentInputItem = null;
        // setItem calls createResult again
        inputSlots.setItem(0, inputItem);

        if(optSpeaker.isEmpty()) {
            Wiretap.LOGGER.error("Could not create speaker item from anvil recipe", new Throwable());
            return;
        }

        ItemStack speaker = optSpeaker.get();


        boolean added = player.getInventory().add(speaker);
        if (!added || !speaker.isEmpty()) {
            player.drop(speaker, false);
        }
    }

}
