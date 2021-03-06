package net.dumbcode.dumblibrary.server.ecs.system.impl;

import net.dumbcode.dumblibrary.server.ecs.ComponentAccess;
import net.dumbcode.dumblibrary.server.ecs.EntityFamily;
import net.dumbcode.dumblibrary.server.ecs.EntityManager;
import net.dumbcode.dumblibrary.server.ecs.component.EntityComponentTypes;
import net.dumbcode.dumblibrary.server.ecs.component.impl.HerdComponent;
import net.dumbcode.dumblibrary.server.ecs.system.EntitySystem;
import net.dumbcode.dumblibrary.server.utils.WorldUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.UUID;

public class HerdSystem implements EntitySystem {

    private Entity[] matchedEntities = new Entity[0];
    private HerdComponent[] herds = new HerdComponent[0];

    @Override
    public void populateEntityBuffers(EntityManager manager) {
        EntityFamily<Entity> family = manager.resolveFamily(EntityComponentTypes.HERD);
        this.herds = family.populateBuffer(EntityComponentTypes.HERD, this.herds);
        this.matchedEntities = family.getEntities();
    }

    @Override
    public void update(World world) {
        for (int i = 0; i < this.herds.length; i++) {
            Entity entity = this.matchedEntities[i];
            HerdComponent herd = this.herds[i];

            if(!herd.isInHerd()) {
                this.tryJoinNewHeard(entity, herd);

                if(!herd.isInHerd()) {
                    this.createNewHerd(entity, herd);
                }
            } else {
                this.moveAsHeard(entity, herd);
            }
        }
    }


    private void tryJoinNewHeard(Entity entity, HerdComponent herd) {
        for (Entity foundEntity : entity.world.getEntitiesInAABBexcluding(entity, new AxisAlignedBB(entity.getPosition()).grow(50, 50, 50), e -> e instanceof ComponentAccess
                && ((ComponentAccess) e).get(EntityComponentTypes.HERD).map(c -> c.herdTypeID.equals(herd.herdTypeID)).orElse(false))) {
            ComponentAccess ca = (ComponentAccess) foundEntity;
            Optional<HerdComponent> component = ca.get(EntityComponentTypes.HERD);

            component.flatMap(HerdComponent::getHerdData).ifPresent(d -> d.addMember(entity.getUniqueID(), herd));
        }
    }

    private void createNewHerd(Entity entity, HerdComponent herd) {
        System.out.println("Created New Herd!");

        herd.herdUUID = UUID.randomUUID();

        herd.getHerdData().ifPresent(d -> {
            d.addMember(entity.getUniqueID(), herd);
            d.leader = entity.getUniqueID();
        });
    }

    //Currently, this moves all the entities 30 blocks away to where the leader is. This could maybe be changed.
    private void moveAsHeard(Entity entity, HerdComponent herd) {
        herd.getHerdData().ifPresent(d -> {
            if(d.leader.equals(entity.getUniqueID()) && d.tryMoveCooldown-- <= 0) {
                for (UUID uuid : d.getMembers()) {
                    WorldUtils.getEntityFromUUID(entity.world, uuid)
                        .filter(e -> e.getDistance(entity) > 30 && e instanceof ComponentAccess)
                        .map(e -> Pair.of(e, ((ComponentAccess)e).get(EntityComponentTypes.HERD)))
                        .ifPresent(pair -> pair.getRight().ifPresent(h -> {
                            ((EntityLiving) entity).getNavigator().tryMoveToEntityLiving(pair.getLeft(), 0.1F);
                            h.getHerdData().ifPresent(hsd -> hsd.tryMoveCooldown = 120);
                        }));
                }
            }
        });
    }

    @SubscribeEvent
    public void onEntityDie(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if(entity instanceof ComponentAccess) {
            Optional<HerdComponent> component = ((ComponentAccess) entity).get(EntityComponentTypes.HERD);
            component.flatMap(HerdComponent::getHerdData).ifPresent(hsd -> {
                if(entity.getUniqueID().equals(hsd.leader)) {
                    hsd.pickNewLeader(entity.world);
                }
                hsd.removeMember(entity.getUniqueID(), component.get());
            });
        }
        Entity source = event.getSource().getTrueSource();
        if(source instanceof ComponentAccess) {
            ((ComponentAccess) source).get(EntityComponentTypes.HERD).flatMap(HerdComponent::getHerdData).ifPresent(h -> h.removeEnemy(entity.getUniqueID()));
        }
    }

    @SubscribeEvent
    public void onEntityDamaged(LivingAttackEvent event) {
        Entity entity = event.getEntity();
        Entity source = event.getSource().getTrueSource();
        if(source !=  null && entity instanceof ComponentAccess) {
            ((ComponentAccess) entity).get(EntityComponentTypes.HERD).flatMap(HerdComponent::getHerdData).ifPresent(d -> {
                if(!d.getMembers().contains(source.getUniqueID())) {
                    d.addEnemy(source.getUniqueID());
                }
            });
        }
    }
}