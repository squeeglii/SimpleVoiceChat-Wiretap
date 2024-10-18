package de.maxhenkel.wiretap.mixin;

import com.mojang.authlib.GameProfile;
import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.utils.HeadUtils;
import de.maxhenkel.wiretap.wiretap.IWiretapDevice;
import de.maxhenkel.wiretap.wiretap.WiretapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PlayerHeadBlock;
import net.minecraft.world.level.block.PlayerWallHeadBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "playerDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"), cancellable = true)
    public void playerDestroy(Level level, Player player, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity, ItemStack itemStack, CallbackInfo ci) {
        if (!(blockState.getBlock() instanceof PlayerHeadBlock || blockState.getBlock() instanceof PlayerWallHeadBlock)) {
            return;
        }
        if (!(blockEntity instanceof SkullBlockEntity skullBlockEntity)) {
            return;
        }

        IWiretapDevice device = (IWiretapDevice) skullBlockEntity;

        switch (device.wiretap$getDeviceType()) {
            case MICROPHONE -> {
                UUID microphone = device.wiretap$getPairId();

                WiretapManager.getInstance().removeMicrophone(microphone);
                ItemStack microphoneItem = HeadUtils.createMicrophone(microphone);
                Block.popResource(level, blockPos, microphoneItem);
                ci.cancel();
            }

            case SPEAKER -> {
                UUID speaker = device.wiretap$getPairId();

                WiretapManager.getInstance().removeSpeaker(speaker);
                ItemStack speakerItem = HeadUtils.createSpeaker(speaker);
                Block.popResource(level, blockPos, speakerItem);
                ci.cancel();
            }
        }
    }

}
