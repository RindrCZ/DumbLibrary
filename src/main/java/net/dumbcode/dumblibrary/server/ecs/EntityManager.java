package net.dumbcode.dumblibrary.server.ecs;

import net.dumbcode.dumblibrary.DumbLibrary;
import net.dumbcode.dumblibrary.server.ecs.component.EntityComponentType;
import net.dumbcode.dumblibrary.server.ecs.system.EntitySystem;
import net.dumbcode.dumblibrary.server.ecs.system.RegisterSystemsEvent;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = DumbLibrary.MODID)
public interface EntityManager extends ICapabilityProvider {
    @SubscribeEvent
    static void onAttachWorldCapabilities(AttachCapabilitiesEvent<World> event) {
        EntityManager.Impl capability = new EntityManager.Impl();
        event.addCapability(new ResourceLocation(DumbLibrary.MODID, "entity_manager"), capability);

        MinecraftForge.EVENT_BUS.post(new RegisterSystemsEvent(event.getObject(), capability.systems));
        for (EntitySystem system : capability.systems) {
            MinecraftForge.EVENT_BUS.register(system);
            system.populateBlockstateBuffers(BlockstateManager.INSTANCE);
        }
    }

    @SubscribeEvent
    static void onWorldTick(TickEvent.WorldTickEvent event) {
        // TODO: We probably need to run this when ecs ticking starts
        if (event.phase == TickEvent.Phase.START) {
            EntityManager entityManager = event.world.getCapability(DumbLibrary.ENTITY_MANAGER, null);
            if (entityManager != null) {
                entityManager.updateSystems(event.world);
            }
        }
    }

    void updateSystems(World world);

    void addEntity(Entity entity);

    void removeEntity(Entity entity);

    EntityFamily<Entity> resolveFamily(EntityComponentType<?, ?>... types);

    class Impl implements EntityManager {
//        private final
        private final List<Entity> managedEntities = new ArrayList<>();
        private final List<EntitySystem> systems = new ArrayList<>();
        private boolean systemsDirty = true;

        @Override
        public void updateSystems(World world) {
            if (this.systemsDirty) {
                this.systemsDirty = false;
                for (EntitySystem system : this.systems) {
                    system.populateEntityBuffers(this);
                }
            }
            for (EntitySystem system : this.systems) {
                system.update(world);
            }
        }

        @Override
        public void addEntity(Entity entity) {
            if (entity instanceof ComponentAccess) {
                this.managedEntities.add(entity);
                this.systemsDirty = true;
            }
        }

        @Override
        public void removeEntity(Entity entity) {
            if (entity instanceof ComponentAccess) {
                this.managedEntities.remove(entity);
                this.systemsDirty = true;
            }
        }

        @Override
        public  EntityFamily<Entity> resolveFamily(EntityComponentType<?, ?>... types) {
            List<Entity> entities = new ArrayList<>(this.managedEntities.size());
            for (Entity entity : this.managedEntities) {
                if (((ComponentAccess) entity).matchesAll(types)) {
                    entities.add(entity);
                }
            }
            return new EntityFamily<>(entities.toArray(new Entity[0]), entity -> (ComponentAccess) entity);
        }

        @Override
        public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == DumbLibrary.ENTITY_MANAGER;
        }

        @Nullable
        @Override
        public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
            if (capability == DumbLibrary.ENTITY_MANAGER) {
                return DumbLibrary.ENTITY_MANAGER.cast(this);
            }
            return null;
        }
    }
}
