package de.maxhenkel.wiretap.wiretap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class DimensionLocation {

    private final ServerLevel level;
    private final BlockPos pos;

    public DimensionLocation(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    public Identifier getDimension() {
        return this.level.dimension().identifier();
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int getX() {
        return this.pos.getX();
    }

    public int getY() {
        return this.pos.getY();
    }

    public int getZ() {
        return this.pos.getZ();
    }

    public boolean isDimension(Level level) {
        return level.dimension().identifier().equals(this.getDimension());
    }

    public double getDistance(Vec3 p) {
        return Math.sqrt(p.distanceToSqr(this.pos.getX(), this.pos.getY(), this.pos.getZ()));
    }

    public boolean isLoaded() {
        return this.level.isLoaded(this.pos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DimensionLocation that = (DimensionLocation) o;

        if (!Objects.equals(this.getDimension(), that.getDimension())) return false;
        return Objects.equals(this.pos, that.pos);
    }

    @Override
    public int hashCode() {
        int result = this.getDimension() != null ? this.getDimension().hashCode() : 0;
        result = 31 * result + (this.pos != null ? this.pos.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "X %s, Y %s, Z %s in %s".formatted(this.getX(), this.getY(), this.getZ(), this.getDimension().toString());
    }
}
