package de.maxhenkel.wiretap.mixin;

import de.maxhenkel.wiretap.item.WiretapDevice;
import de.maxhenkel.wiretap.utils.HeadUtils;
import de.maxhenkel.wiretap.wiretap.DeviceType;
import de.maxhenkel.wiretap.wiretap.IWiretapDeviceHolder;
import de.maxhenkel.wiretap.wiretap.WiretapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    @Inject(method = "loadAdditional", at = @At("RETURN"))
    public void load(ValueInput valueInput, CallbackInfo ci) {
        this.deviceData = valueInput.read(HeadUtils.NBT_DEVICE, WiretapDevice.CODEC).orElse(null);

        if(this.level != null && !this.level.isClientSide) {
            WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        }
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    public void save(ValueOutput valueOutput, CallbackInfo ci) {
        if(this.deviceData != null) {
            valueOutput.store(HeadUtils.NBT_DEVICE, this.deviceData.getSerialisationCodec(), this.deviceData);
        }
    }

    // item conversion ---
    @Inject(method = "applyImplicitComponents(Lnet/minecraft/core/component/DataComponentGetter;)V", at = @At("RETURN"))
    protected void applyExtraComponents(DataComponentGetter dataComponentGetter, CallbackInfo ci) {
        // Mark both types as checked so they get removed from the held components.
        CustomData data = dataComponentGetter.get(DataComponents.CUSTOM_DATA);

        if(data == null) {
            return;
        }

        Optional<WiretapDevice> wiretapDevice = data.copyTag().read(HeadUtils.NBT_DEVICE, WiretapDevice.CODEC);

        if(wiretapDevice.isEmpty())
            return;

        this.deviceData = wiretapDevice.get();

        if(this.wiretap$getDeviceType() != DeviceType.NON_WIRETAP && this.level != null && !this.level.isClientSide) {
            WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        }
    }

    @Inject(method = "collectImplicitComponents(Lnet/minecraft/core/component/DataComponentMap$Builder;)V", at = @At("RETURN"))
    protected void collectExtraComponents(DataComponentMap.Builder builder, CallbackInfo ci) {
        if(this.deviceData == null) return;

        CustomData data = this.deviceData.saveToNewCustomData();
        builder.set(DataComponents.CUSTOM_DATA, data);
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
