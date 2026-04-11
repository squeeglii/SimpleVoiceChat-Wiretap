package de.maxhenkel.wiretap.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import de.maxhenkel.wiretap.wiretap.DeviceType;
import de.maxhenkel.wiretap.wiretap.IWiretapDeviceHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(ServerGamePacketListenerImpl.class)
public class EnhancedPickBlockMixin {

    // If picked block is a speaker / microphone (is skull + has specific data inside skull)
    // then it should always pick-block with the device data included, no matter
    // the ctrl modifier.
    @ModifyExpressionValue(
            method = "handlePickItemFromBlock(Lnet/minecraft/network/protocol/game/ServerboundPickItemFromBlockPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundPickItemFromBlockPacket;includeData()Z"
    ))
    private boolean includeDataOnRadio(boolean original, @Local(name = "level") ServerLevel level, @Local(name = "pos") BlockPos pos) {
        Optional<SkullBlockEntity> optSkull = level.getBlockEntity(pos, BlockEntityType.SKULL);

        if(optSkull.isEmpty())
            return original;

        SkullBlockEntity skull = optSkull.get();

        if(!(skull instanceof IWiretapDeviceHolder device))
            return original; // shouldn't be possible because mixins but hey.

        return original || device.wiretap$getDeviceType() != DeviceType.NON_WIRETAP;
    }

    // this should be compatible with the radio addon as it's just OR chains ^^

}
