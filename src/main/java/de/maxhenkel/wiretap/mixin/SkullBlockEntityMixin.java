package de.maxhenkel.wiretap.mixin;

import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.item.WiretapDevice;
import de.maxhenkel.wiretap.utils.HeadUtils;
import de.maxhenkel.wiretap.wiretap.DeviceType;
import de.maxhenkel.wiretap.wiretap.IWiretapDeviceHolder;
import de.maxhenkel.wiretap.wiretap.WiretapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

@Mixin(SkullBlockEntity.class)
public class SkullBlockEntityMixin extends BlockEntity implements IWiretapDeviceHolder {

    @Unique private WiretapDevice deviceData = null;

    public SkullBlockEntityMixin(BlockPos blockPos, BlockState blockState) {
        super(BlockEntityType.SKULL, blockPos, blockState);
    }

    // block loading / saving ----
    @Inject(method = "load", at = @At("RETURN"))
    public void load(CompoundTag compoundTag, CallbackInfo ci) {
        if(!compoundTag.contains(HeadUtils.NBT_DEVICE, Tag.TAG_COMPOUND)) {
            this.deviceData = null;
            return;
        }

        CompoundTag tag = compoundTag.getCompound(HeadUtils.NBT_DEVICE);

        this.deviceData = WiretapDevice.CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(err -> Wiretap.LOGGER.error("Failed to read wiretap device ({}): {}", tag, err))
                .orElse(null);

        // todo: do I check for nulls?

        if(this.level != null && !this.level.isClientSide) {
            WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        }
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    public void save(CompoundTag compoundTag, CallbackInfo ci) {
        if(this.deviceData != null) {
            compoundTag.put(HeadUtils.NBT_DEVICE, this.deviceData.getSerialisationCodec().encodeStart(NbtOps.INSTANCE, this.deviceData).getOrThrow(false, Wiretap.LOGGER::error));
        }
    }

    // 1.20.1 -- I believe these below are both handled by loading the skull data into
    // the right section of the NBT. (WiretapDevice#serialiseIntoItemStack)
    // Previous versions, I think this was fucked as the difference between item data and the
    // block entity data was forgotten.

    /*
    // item conversion ---
    @Inject(method = "applyImplicitComponents", at = @At("RETURN"))
    protected void applyExtraComponents(DataComponentInput dataComponentInput, CallbackInfo ci) {
        // Mark both types as checked so they get removed from the held components.
        CustomData data = dataComponentInput.get(DataComponents.CUSTOM_DATA);
        if(data == null) {
            return;
        }

        CompoundTag dataCompound = data.copyTag();
        if(!dataCompound.contains(HeadUtils.NBT_DEVICE, Tag.TAG_COMPOUND)) {
            this.deviceData = null;
            return;
        }

        CompoundTag tag = dataCompound.getCompound(HeadUtils.NBT_DEVICE);
        Optional<WiretapDevice> wiretapDevice = WiretapDevice.CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(err -> Wiretap.LOGGER.error("Failed to read wiretap device ({}): {}", tag, err));

        if(wiretapDevice.isEmpty())
            return;

        this.deviceData = wiretapDevice.get();

        if(this.wiretap$getDeviceType() != DeviceType.NON_WIRETAP && this.level != null && !this.level.isClientSide) {
            WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        }
    }
     */

    /*
    @Inject(method = "collectImplicitComponents(Lnet/minecraft/core/component/DataComponentMap$Builder;)V", at = @At("RETURN"))
    protected void collectExtraComponents(DataComponentMap.Builder builder, CallbackInfo ci) {
        if(this.deviceData == null) return;

        CustomData data = this.deviceData.saveToNewCustomData();
        builder.set(DataComponents.CUSTOM_DATA, data);
    }
     */



    @Override
    public void setLevel(Level newLevel) {
        Level oldLevel = level;
        super.setLevel(newLevel);

        // This is needed otherwise the first loadAdditional on world load doesn't
        // actually open the channel and register this.
        if (oldLevel == null && newLevel != null && !newLevel.isClientSide) {
            WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        }
    }

    @Override
    @Nonnull
    public DeviceType wiretap$getDeviceType() {
        return this.deviceData == null ? DeviceType.NON_WIRETAP : this.deviceData.getDeviceType();
    }

    @Override
    public UUID wiretap$getPairId() {
        if(this.deviceData == null)
            throw new IllegalStateException("Failed to check device type before getting pair id");
        return this.deviceData.getPairUUID();
    }

    @Override
    public Optional<WiretapDevice> wiretap$getDeviceData() {
        return Optional.ofNullable(this.deviceData);
    }
}
