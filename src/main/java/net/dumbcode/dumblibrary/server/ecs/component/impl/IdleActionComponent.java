package net.dumbcode.dumblibrary.server.ecs.component.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.dumbcode.dumblibrary.server.animation.objects.Animation;
import net.dumbcode.dumblibrary.server.ecs.component.EntityComponent;
import net.dumbcode.dumblibrary.server.ecs.component.EntityComponentStorage;
import net.dumbcode.dumblibrary.server.utils.CollectorUtils;
import net.dumbcode.dumblibrary.server.utils.StreamUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class IdleActionComponent extends EntityComponent {
    public int soundTicks;
    public int animationTicks;

    public Animation sittingAnimation;
    public final List<Animation> idleAnimations = new ArrayList<>();
    public final List<Animation> movementAnimation = new ArrayList<>();

    @Override
    public NBTTagCompound serialize(NBTTagCompound compound) {
        compound.setString("SittingAnimation", sittingAnimation.getKey().toString());
        compound.setTag("Movement", this.movementAnimation.stream().map(a -> a.getKey().toString()).collect(CollectorUtils.toNBTList(NBTTagString::new)));
        compound.setTag("Animations", this.idleAnimations.stream().map(a -> a.getKey().toString()).collect(CollectorUtils.toNBTList(NBTTagString::new)));
        return super.serialize(compound);
    }

    @Override
    public void deserialize(NBTTagCompound compound) {
        this.sittingAnimation = new Animation(new ResourceLocation(compound.getString("SittingAnimation")));
        this.idleAnimations.clear();
        StreamUtils.stream(compound.getTagList("Animations", Constants.NBT.TAG_STRING)).map(b -> new Animation(new ResourceLocation(((NBTTagString)b).getString()))).forEach(this.movementAnimation::add);
        this.movementAnimation.clear();
        StreamUtils.stream(compound.getTagList("Movement", Constants.NBT.TAG_STRING)).map(b -> new Animation(new ResourceLocation(((NBTTagString)b).getString()))).forEach(this.movementAnimation::add);
        super.deserialize(compound);
    }

    @Setter
    @Getter
    @Accessors(chain = true)
    public static class Storage implements EntityComponentStorage<IdleActionComponent> {

        public List<Supplier<Animation>> idleAnimations = new ArrayList<>();
        public Supplier<Animation> sittingAnimation;
        public List<Supplier<Animation>> movementAnimations = new ArrayList<>();


        @Override
        public void constructTo(IdleActionComponent component) {
            component.sittingAnimation = sittingAnimation.get();
            this.movementAnimations.stream().map(Supplier::get).forEach(component.movementAnimation::add);
            this.idleAnimations.stream().map(Supplier::get).forEach(component.idleAnimations::add);
        }
    }
}
