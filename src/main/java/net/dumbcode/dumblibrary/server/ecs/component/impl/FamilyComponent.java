package net.dumbcode.dumblibrary.server.ecs.component.impl;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.dumbcode.dumblibrary.server.ecs.ComponentAccess;
import net.dumbcode.dumblibrary.server.ecs.FamilySavedData;
import net.dumbcode.dumblibrary.server.ecs.component.EntityComponent;
import net.dumbcode.dumblibrary.server.ecs.component.EntityComponentStorage;
import net.dumbcode.dumblibrary.server.ecs.component.EntityComponentTypes;
import net.dumbcode.dumblibrary.server.ecs.component.additionals.CanBreedComponent;
import net.dumbcode.dumblibrary.server.ecs.component.additionals.GatherEnemiesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;


public class FamilyComponent extends EntityComponent implements CanBreedComponent {
    @Getter @Setter private UUID familyUUID;
    @Getter @Setter private ResourceLocation familyTypeId;
    private boolean mateForLife;
    private FamilySavedData dataCache;

    public FamilySavedData getDataCache() {
        if(this.dataCache == null) {
            this.dataCache = FamilySavedData.getData(this.familyUUID);
        }
        return this.dataCache;
    }

    @Override
    public NBTTagCompound serialize(NBTTagCompound compound) {
        compound.setUniqueId("family_uuid", this.familyUUID);
        compound.setString("family_type", this.familyTypeId.toString());
        compound.setBoolean("mate_for_life", this.mateForLife);
        return super.serialize(compound);
    }

    @Override
    public void deserialize(NBTTagCompound compound) {
        this.familyUUID = compound.getUniqueId("family_uuid");
        this.familyTypeId = new ResourceLocation(compound.getString("family_type"));
        this.mateForLife = compound.getBoolean("mate_for_life");
        super.deserialize(compound);
    }

    @Override
    public boolean canBreedWith(ComponentAccess otherEntity) {
        return otherEntity.get(EntityComponentTypes.FAMILY).map(f -> {
            if(this.mateForLife && !this.familyUUID.equals(f.familyUUID)) {
                return false;
            }
            return !this.getDataCache().getChildren().contains(((Entity)otherEntity).getUniqueID());
        }).orElse(true);
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Storage implements EntityComponentStorage<FamilyComponent> {

        private ResourceLocation familyType;

        @Override
        public void constructTo(FamilyComponent component) {
            component.familyTypeId = this.familyType;
        }

        @Override
        public void writeJson(JsonObject json) {
            json.addProperty("family_type", this.familyType.toString());
        }

        @Override
        public void readJson(JsonObject json) {
            this.familyType = new ResourceLocation(JsonUtils.getString(json, "family_type"));
        }
    }
}
