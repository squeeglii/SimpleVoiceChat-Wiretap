package de.maxhenkel.wiretap.mixin;

import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.item.WiretapDevice;
import de.maxhenkel.wiretap.utils.HeadUtils;
import de.maxhenkel.wiretap.wiretap.DeviceType;
import de.maxhenkel.wiretap.wiretap.IWiretapDeviceHolder;
import de.maxhenkel.wiretap.wiretap.WiretapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

import java.util.Optional;
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

        IWiretapDeviceHolder device = (IWiretapDeviceHolder) skullBlockEntity;
        Optional<WiretapDevice> optDeviceData = device.wiretap$getDeviceData();

        if(optDeviceData.isEmpty())
            return;   // Either malformed data or just not a wiretap head. Hard to tell without a mess.

        WiretapDevice deviceData = optDeviceData.get();

        if(deviceData.getDeviceType() == DeviceType.NON_WIRETAP) {
            Wiretap.LOGGER.debug("Discarding 'NON_WIRETAP' but present device data drop.");
            return;
        }

        UUID devicePairId = deviceData.getPairUUID();

        switch (device.wiretap$getDeviceType()) {
            case MICROPHONE -> WiretapManager.getInstance().removeMicrophone(devicePairId);
            case SPEAKER -> WiretapManager.getInstance().removeSpeaker(devicePairId);
        }

        HeadUtils.createHead(deviceData).ifPresent(
                stack -> Block.popResource(level, blockPos, stack)
        );

        ci.cancel();
    }

}
