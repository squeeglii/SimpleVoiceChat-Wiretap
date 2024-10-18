package de.maxhenkel.wiretap.mixin;

import de.maxhenkel.wiretap.item.component.SpeakerComponent;
import de.maxhenkel.wiretap.utils.HeadUtils;
import de.maxhenkel.wiretap.wiretap.DeviceType;
import de.maxhenkel.wiretap.wiretap.IWiretapDevice;
import de.maxhenkel.wiretap.wiretap.IRangeOverridable;
import de.maxhenkel.wiretap.wiretap.WiretapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.component.ResolvableProfile;
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

import java.util.UUID;

@Mixin(SkullBlockEntity.class)
public class SkullBlockEntityMixin extends BlockEntity implements IRangeOverridable, IWiretapDevice {

    @Unique
    private UUID pairId = null;

    @Unique
    private DeviceType deviceType = DeviceType.NON_WIRETAP;

    @Unique
    private Float rangeOverride = null;

    public SkullBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Inject(method = "setOwner", at = @At("RETURN"))
    public void setOwner(ResolvableProfile resolvableProfile, CallbackInfo ci) {
        // Shouldn't be a problem anymore - only visual data is stored in the texture data.
        //if (level != null && !level.isClientSide) {
        //    WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        //}
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    public void load(CompoundTag compoundTag, HolderLookup.Provider provider, CallbackInfo ci) {

        // Check if new format is present.
        if(!compoundTag.contains(HeadUtils.NBT_DEVICE, Tag.TAG_FLOAT)) {

            // Check if old format is present - try and harvest the old data if possible.
            if(compoundTag.contains(HeadUtils.NBT_SPEAKER_RANGE, Tag.TAG_FLOAT)) {
                this.rangeOverride = compoundTag.getFloat(HeadUtils.NBT_SPEAKER_RANGE);

                // TODO: Try and harvest profile data.

                return;
            }


            // Sanity check, mark device as definitely not wiretap owned if it
            // has a weird data structure.
            this.deviceType = DeviceType.NON_WIRETAP;
            return;
        }

        // Load new format.

        if(this.level == null) return;
        if(this.level.isClientSide) return;

        CompoundTag speakerData = compoundTag.getCompound(HeadUtils.NBT_DEVICE);

        this.pairId = speakerData.getUUID(HeadUtils.NBT_PAIR_ID);
        this.deviceType = speakerData.getBoolean(HeadUtils.NBT_IS_SPEAKER)
                ? DeviceType.SPEAKER
                : DeviceType.MICROPHONE;

        if(this.deviceType == DeviceType.SPEAKER) {
            this.rangeOverride = speakerData.contains(HeadUtils.NBT_SPEAKER_RANGE, Tag.TAG_FLOAT)
                    ? speakerData.getFloat(HeadUtils.NBT_SPEAKER_RANGE)
                    : null;
        }

        WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    public void save(CompoundTag compoundTag, HolderLookup.Provider provider, CallbackInfo ci) {

        if(this.deviceType == null || this.deviceType == DeviceType.NON_WIRETAP) {
            return;
        }

        CompoundTag deviceData = new CompoundTag();

        deviceData.putUUID(HeadUtils.NBT_PAIR_ID, this.pairId);
        deviceData.putBoolean(HeadUtils.NBT_IS_SPEAKER, this.deviceType == DeviceType.SPEAKER);

        if(this.deviceType == DeviceType.SPEAKER || this.rangeOverride != null) {
            deviceData.putFloat(HeadUtils.NBT_SPEAKER_RANGE, this.rangeOverride);
        }

        compoundTag.put(HeadUtils.NBT_DEVICE, deviceData);
    }

    @Override
    public void setLevel(Level newLevel) {
        Level oldLevel = level;
        super.setLevel(newLevel);

        if (oldLevel == null && newLevel != null && !newLevel.isClientSide) {
            WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        }
    }

    @Override
    public float wiretap$getRangeOverride() {
        return this.rangeOverride == null
                ? -1.0f
                : this.rangeOverride;
    }

    @Override
    public DeviceType wiretap$getDeviceType() {
        return this.deviceType;
    }

    @Override
    public UUID wiretap$getPairId() {
        return this.pairId;
    }
}
