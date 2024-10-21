package de.maxhenkel.wiretap.mixin;

import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.item.WiretapDataComponents;
import de.maxhenkel.wiretap.item.component.MicrophoneComponent;
import de.maxhenkel.wiretap.item.component.SpeakerComponent;
import de.maxhenkel.wiretap.utils.HeadUtils;
import de.maxhenkel.wiretap.wiretap.DeviceType;
import de.maxhenkel.wiretap.wiretap.IRangeOverridable;
import de.maxhenkel.wiretap.wiretap.IWiretapDevice;
import de.maxhenkel.wiretap.wiretap.WiretapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
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

import java.util.UUID;

@Mixin(SkullBlockEntity.class)
public class SkullBlockEntityMixin extends BlockEntity implements IRangeOverridable, IWiretapDevice {

    @Unique private UUID pairId = null;
    @Unique private DeviceType deviceType = null;
    @Unique private Float rangeOverride = null;

    public SkullBlockEntityMixin(BlockPos blockPos, BlockState blockState) {
        super(BlockEntityType.SKULL, blockPos, blockState);
    }


    @Inject(method = "loadAdditional", at = @At("RETURN"))
    public void load(CompoundTag compoundTag, HolderLookup.Provider provider, CallbackInfo ci) {
        // Check if new format is present.
        if(!compoundTag.contains(HeadUtils.NBT_DEVICE)) {
            // Sanity check, mark device as definitely not wiretap owned if it
            // has a weird data structure.
            this.deviceType = DeviceType.NON_WIRETAP;
            return;
        }

        // Load new format.
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

        if(this.level != null && !this.level.isClientSide) {
            WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        }
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    public void save(CompoundTag compoundTag, HolderLookup.Provider provider, CallbackInfo ci) {
        if(this.deviceType == null || this.deviceType == DeviceType.NON_WIRETAP) {
            return;
        }

        CompoundTag deviceData = new CompoundTag();

        deviceData.putUUID(HeadUtils.NBT_PAIR_ID, this.pairId);
        deviceData.putBoolean(HeadUtils.NBT_IS_SPEAKER, this.deviceType == DeviceType.SPEAKER);

        if(this.deviceType == DeviceType.SPEAKER || this.wiretap$isRangeOverriden()) {
            deviceData.putFloat(HeadUtils.NBT_SPEAKER_RANGE, this.rangeOverride);
        }

        compoundTag.put(HeadUtils.NBT_DEVICE, deviceData);
    }

    @Inject(method = "applyImplicitComponents(Lnet/minecraft/world/level/block/entity/BlockEntity$DataComponentInput;)V", at = @At("RETURN"))
    protected void applyExtraComponents(DataComponentInput dataComponentInput, CallbackInfo ci) {
        // Mark both types as checked so they get removed from the held components.
        SpeakerComponent speakerComponent = dataComponentInput.get(WiretapDataComponents.SPEAKER);
        MicrophoneComponent microphoneComponent = dataComponentInput.get(WiretapDataComponents.MICROPHONE);

        // Speaker takes priority
        if(speakerComponent != null) {
            this.deviceType = DeviceType.SPEAKER;
            this.pairId = speakerComponent.pairUUID();
            this.rangeOverride = speakerComponent.range();

            if(this.level != null && !this.level.isClientSide)
                WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
            return;
        }

        if(microphoneComponent != null) {
            this.deviceType = DeviceType.MICROPHONE;
            this.pairId = microphoneComponent.pairUUID();
            this.rangeOverride = null;

            if(this.level != null && !this.level.isClientSide)
                WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
            return;
        }
    }

    @Inject(method = "collectImplicitComponents(Lnet/minecraft/core/component/DataComponentMap$Builder;)V", at = @At("RETURN"))
    protected void collectExtraComponents(DataComponentMap.Builder builder, CallbackInfo ci) {
        switch (this.deviceType) {
            case SPEAKER -> builder.set(WiretapDataComponents.SPEAKER, new SpeakerComponent(this.pairId, this.rangeOverride));
            case MICROPHONE -> builder.set(WiretapDataComponents.MICROPHONE, new MicrophoneComponent(this.pairId));
        }
    }

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
    public float wiretap$getRangeOverride() {
        return this.rangeOverride == null
                ? -1.0f
                : this.rangeOverride;
    }

    @Override
    public boolean wiretap$isRangeOverriden() {
        return this.rangeOverride != null;
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
