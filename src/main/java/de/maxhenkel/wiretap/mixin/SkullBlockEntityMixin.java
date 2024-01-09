package de.maxhenkel.wiretap.mixin;

import com.mojang.authlib.GameProfile;
import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.utils.HeadUtils;
import de.maxhenkel.wiretap.wiretap.IRangeOverridable;
import de.maxhenkel.wiretap.wiretap.WiretapManager;
import net.minecraft.core.BlockPos;
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

@Mixin(SkullBlockEntity.class)
public class SkullBlockEntityMixin extends BlockEntity implements IRangeOverridable {

    @Unique
    private Float rangeOverride = null;

    public SkullBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Inject(method = "setOwner", at = @At("RETURN"))
    public void setOwner(GameProfile gameProfile, CallbackInfo ci) {
        if (level != null && !level.isClientSide) {
            WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    public void load(CompoundTag compoundTag, CallbackInfo ci) {
        if(compoundTag.contains(HeadUtils.NBT_SPEAKER_RANGE, Tag.TAG_FLOAT)) {
            this.rangeOverride = compoundTag.getFloat(HeadUtils.NBT_SPEAKER_RANGE);
        }

        if (this.level != null && !this.level.isClientSide) {
            WiretapManager.getInstance().onLoadHead((SkullBlockEntity) (Object) this);
        }
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    public void save(CompoundTag compoundTag, CallbackInfo ci) {

        if(this.rangeOverride != null) {
            compoundTag.putFloat(HeadUtils.NBT_SPEAKER_RANGE, this.rangeOverride);
        }

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

}
