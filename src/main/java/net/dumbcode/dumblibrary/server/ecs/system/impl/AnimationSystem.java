package net.dumbcode.dumblibrary.server.ecs.system.impl;

import net.dumbcode.dumblibrary.server.ecs.ComponentAccess;
import net.dumbcode.dumblibrary.server.ecs.EntityManager;
import net.dumbcode.dumblibrary.server.ecs.component.EntityComponentTypes;
import net.dumbcode.dumblibrary.server.ecs.component.impl.AnimationComponent;
import net.dumbcode.dumblibrary.server.ecs.system.EntitySystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public enum AnimationSystem implements EntitySystem {
    INSTANCE;

    private AnimationComponent<?>[] components = null;

    @Override
    public void populateEntityBuffers(EntityManager manager) {
        this.components = manager.resolveFamily(EntityComponentTypes.ANIMATION).populateBuffer(EntityComponentTypes.ANIMATION, this.components);
    }

    private void updateComponent(AnimationComponent<?> component) {
        component.getFutureAnimations().removeIf(AnimationComponent.FutureAnimation::tick);
    }

    @Override
    public void update(World world) {
        for (AnimationComponent<?> component : this.components) {
            this.updateComponent(component);
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onClientWorldTick(TickEvent.ClientTickEvent event) {
        World world = Minecraft.getMinecraft().world;
        if(world != null && !Minecraft.getMinecraft().isGamePaused()) {
            for (Entity entity : world.loadedEntityList) {
                if(entity instanceof ComponentAccess) {
                    ((ComponentAccess) entity).get(EntityComponentTypes.ANIMATION).ifPresent(this::updateComponent);
                }
            }
        }
    }
}
