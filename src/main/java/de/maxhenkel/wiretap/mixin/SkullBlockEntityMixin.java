package de.maxhenkel.wiretap.mixin;

import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.item.WiretapDataComponents;
import de.maxhenkel.wiretap.item.component.MicrophoneComponent;
import de.maxhenkel.wiretap.item.component.SpeakerComponent;
import de.maxhenkel.wiretap.utils.HeadUtils;
import de.maxhenkel.wiretap.wiretap.DeviceType;
import de.maxhenkel.wiretap.wiretap.IWiretapDevice;
import de.maxhenkel.wiretap.wiretap.IRangeOverridable;
import de.maxhenkel.wiretap.wiretap.WiretapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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

    @Unique
    private UUID pairId = null;

    @Unique
    private DeviceType deviceType = DeviceType.NON_WIRETAP;

    @Unique
    private Float rangeOverride = null;

    public SkullBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }


    @Inject(method = "loadAdditional", at = @At("RETURN"))
    public void load(CompoundTag compoundTag, HolderLookup.Provider provider, CallbackInfo ci) {
        Wiretap.LOGGER.info("NBT LOAD");
        Wiretap.LOGGER.info("%s".formatted(NbtUtils.prettyPrint(compoundTag)));

        // Check if new format is present.
        if(!compoundTag.contains(HeadUtils.NBT_DEVICE)) {
            Wiretap.LOGGER.info("no modern format device");

            // Check if old format is present - try and harvest the old data if possible.
            if(compoundTag.contains(HeadUtils.NBT_SPEAKER_RANGE, Tag.TAG_FLOAT)) {
                Wiretap.LOGGER.info("old speaker format found!!");
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
        Wiretap.LOGGER.info("doing level checks");

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


        if(this.level != null && !this.level.isClientSide) {
            Wiretap.LOGGER.info("Loading from NBT Load...");
            WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        } else {
            Wiretap.LOGGER.info("This was all client side! Ignore.");
        }
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    public void save(CompoundTag compoundTag, HolderLookup.Provider provider, CallbackInfo ci) {
        Wiretap.LOGGER.info("NBT SAVE");

        if(this.deviceType == null || this.deviceType == DeviceType.NON_WIRETAP) {
            Wiretap.LOGGER.warn("Non Wiretap or Null");
            Wiretap.LOGGER.info("%s".formatted(NbtUtils.prettyPrint(compoundTag)));
            return;
        }

        CompoundTag deviceData = new CompoundTag();

        deviceData.putUUID(HeadUtils.NBT_PAIR_ID, this.pairId);
        deviceData.putBoolean(HeadUtils.NBT_IS_SPEAKER, this.deviceType == DeviceType.SPEAKER);

        if(this.deviceType == DeviceType.SPEAKER || this.rangeOverride != null) {
            deviceData.putFloat(HeadUtils.NBT_SPEAKER_RANGE, this.rangeOverride);
        }

        compoundTag.put(HeadUtils.NBT_DEVICE, deviceData);
        Wiretap.LOGGER.info("%s".formatted(NbtUtils.prettyPrint(compoundTag)));
    }

    @Inject(method = "applyImplicitComponents(Lnet/minecraft/world/level/block/entity/BlockEntity$DataComponentInput;)V", at = @At("RETURN"))
    protected void applyExtraComponents(DataComponentInput dataComponentInput, CallbackInfo ci) {
        // Speaker takes priority - a block having both is unsupported and the microphone will
        // be ignored.

        Wiretap.LOGGER.info("APPLY EXTRA");

        SpeakerComponent speakerComponent = dataComponentInput.get(WiretapDataComponents.SPEAKER);
        if(speakerComponent != null) {
            Wiretap.LOGGER.info("SPEAKER");
            this.deviceType = DeviceType.SPEAKER;
            this.rangeOverride = speakerComponent.range();
            this.pairId = speakerComponent.pairUUID();

            if(this.level != null && !this.level.isClientSide)
                WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
            return;
        }

        MicrophoneComponent microphoneComponent = dataComponentInput.get(WiretapDataComponents.MICROPHONE);
        if(microphoneComponent != null) {
            Wiretap.LOGGER.info("MICROPHONE");
            this.deviceType = DeviceType.MICROPHONE;
            this.rangeOverride = null;
            this.pairId = microphoneComponent.pairUUID();
            if(this.level != null && !this.level.isClientSide)
                WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
            return;
        }
    }

    @Inject(method = "collectImplicitComponents(Lnet/minecraft/core/component/DataComponentMap$Builder;)V", at = @At("RETURN"))
    protected void collectExtraComponents(DataComponentMap.Builder builder, CallbackInfo ci) {
        Wiretap.LOGGER.info("COLLECT EXTRA");

        switch (this.deviceType) {
            case SPEAKER -> {
                Wiretap.LOGGER.info("SPEAKER");
                SpeakerComponent component = new SpeakerComponent(this.pairId, this.rangeOverride);
                builder.set(WiretapDataComponents.SPEAKER, component);
            }

            case MICROPHONE -> {
                Wiretap.LOGGER.info("MICROPHONE");
                MicrophoneComponent component = new MicrophoneComponent(this.pairId);
                builder.set(WiretapDataComponents.MICROPHONE, component);
            }
        }
    }

    @Inject(method = "removeComponentsFromTag(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    protected void removeExtraComponentsFromTag(CompoundTag compoundTag, CallbackInfo ci) {
        Wiretap.LOGGER.info("REMOVE EXTRA");
        Wiretap.LOGGER.info("%s".formatted(NbtUtils.prettyPrint(compoundTag)));
        compoundTag.remove(HeadUtils.NBT_DEVICE);
    }

    @Override
    public void setLevel(Level newLevel) {
        Level oldLevel = level;
        super.setLevel(newLevel);

        // this actually seems to cause issues?
        if (oldLevel == null && newLevel != null && !newLevel.isClientSide) {
            Wiretap.LOGGER.info("SETTING LEVEL LOAD HEAD");
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
