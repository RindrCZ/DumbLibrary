package net.dumbcode.dumblibrary.server.animation.objects;

import lombok.Getter;
import net.dumbcode.dumblibrary.client.model.tabula.TabulaModel;
import net.dumbcode.dumblibrary.server.TickHandler;
import net.dumbcode.dumblibrary.server.animation.interpolation.Interpolation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.vecmath.Vector3f;
import java.util.*;
import java.util.function.Function;

@Getter
public class AnimationWrap {
    protected AnimationEntry entry;

    private final Function<Animation, List<PoseData>> animationDataGetter;
    private final Function<String, CubeWrapper> cuberef;
    private Function<String, AnimatableCube> anicubeRef;
    private final Collection<String> cubeNames;

    private final Interpolation interpolation;

    private final Deque<PoseData> poseStack = new ArrayDeque<>();

    private final float totalPoseTime;

    private final Object object;

    private float animationTicks;
    private float animationPartialTicks;

    private float maxTicks;
    private float tick;
    private float ci;

    private boolean invalidated;

    public AnimationWrap (
        AnimationEntry animation, Function<Animation, List<PoseData>> animationDataGetter,
        Function<String, CubeWrapper> cuberef, Function<String, AnimatableCube> anicubeRef,
        Collection<String> cubeNames, Object object) {
        this.animationDataGetter = animationDataGetter;
        this.cuberef = cuberef;
        this.anicubeRef = anicubeRef;
        this.cubeNames = cubeNames;
        this.animationTicks = TickHandler.getTicks();
        this.entry = animation;
        this.object = object;
        this.poseStack.addAll(this.animationDataGetter.apply(animation.getAnimation()));
        if(this.poseStack.isEmpty()) {
            this.invalidated = true;
        } else {
            this.maxTicks = this.getData().getTime();
        }
        this.interpolation = animation.getInterpolation();

        float tpt = 0;
        for (PoseData poseData : this.poseStack) {
            tpt += poseData.getTime();
        }
        this.totalPoseTime = tpt;

        if(!this.invalidated) {
            this.incrementVecs(false);
        }
    }

    public void tick(float partialTicks) {
        if (this.invalidated) { // && !this.entry.hold
            return;
        }

        float perc = this.tick / this.maxTicks;

//        this.ci = MathHelper.clamp(this.entry.isUseInertia() && perc <= 1F ? (float) (Math.sin(Math.PI * (perc - 0.5D)) * 0.5D + 0.5D) : perc, 0, 1);

        this.ci = MathHelper.clamp(perc, 0, 1);

        float factor = this.entry.getDegreeFactor().tryApply(object, AnimationFactor.Type.ANGLE, partialTicks);

        for (String partName : this.cubeNames) {
            CubeWrapper cubeWrapper = Objects.requireNonNull(this.cuberef.apply(partName));
            AnimatableCube cube = this.anicubeRef.apply(partName);

            float[] interpolatedRotation = this.interpolation.getInterpRot(cubeWrapper, ci);
            float[] interpolatedPosition = this.interpolation.getInterpPos(cubeWrapper, ci);

            float[] rotation = cube.getDefaultRotation();
            cube.addRotation(
                (interpolatedRotation[0] - rotation[0]) * factor,
                (interpolatedRotation[1] - rotation[1]) * factor,
                (interpolatedRotation[2] - rotation[2]) * factor
            );


            float[] positions = cube.getDefaultRotationPoint();
            cube.addRotationPoint(
                interpolatedPosition[0] - positions[0],
                interpolatedPosition[1] - positions[1],
                interpolatedPosition[2] - positions[2]
            );
        }

        float timeModifier = 1f;
        if(this.entry.getTime() > 0) {
            timeModifier = this.entry.getTime() / this.totalPoseTime;
        }

        timeModifier /= this.entry.getSpeedFactor().tryApply(object, AnimationFactor.Type.SPEED, partialTicks);

        float ticks = TickHandler.getTicks();
        this.tick += ((ticks-this.animationTicks) + (partialTicks-this.animationPartialTicks)) / timeModifier;//todo: Check that looping and holding work

        //Make sure to catchup to correct render
        while (!this.invalidated && this.tick >= this.maxTicks && (!this.entry.isHold() || this.poseStack.size() > 1)) {
            this.poseStack.pop();
            if (this.poseStack.isEmpty()) {
                if (this.entry.getTime() == AnimationLayer.LOOP) {
                    this.poseStack.addAll(this.animationDataGetter.apply(this.entry.getAnimation()));
                } else {
                    this.invalidated = true;
                }
            }
            if (!this.invalidated) {
                this.tick -= this.maxTicks;
                this.maxTicks = this.getData().getTime();
                this.incrementVecs(true);
            }
        }
        this.animationTicks = ticks;
        this.animationPartialTicks = partialTicks;
    }

    @SideOnly(Side.CLIENT)
    public void setFromModel(TabulaModel model) {
        this.cubeNames.clear();
        this.cubeNames.addAll(model.getAllCubesNames());
        this.anicubeRef = model::getCube;
    }

    private void incrementVecs(boolean updatePrevious) {
        for (Map.Entry<String, CubeReference> mapEntry : this.getData().getCubes().entrySet()) {
            CubeWrapper cube = Objects.requireNonNull(this.cuberef.apply(mapEntry.getKey()));
            Vector3f cr = cube.getRotation();
            Vector3f pr = cube.getPrevRotation();

            Vector3f np = cube.getRotationPoint();
            Vector3f pp = cube.getPrevRotationPoint();

            if (updatePrevious) {
                pr.x = cr.x;
                pr.y = cr.y;
                pr.z = cr.z;

                pp.x = np.x;
                pp.y = np.y;
                pp.z = np.z;
            }

            CubeReference next = mapEntry.getValue();
            cr.x = next.getRotationX();
            cr.y = next.getRotationY();
            cr.z = next.getRotationZ();
            np.x = next.getRotationPointX();
            np.y = next.getRotationPointY();
            np.z = next.getRotationPointZ();
        }
    }

    public void onFinish() {
        this.invalidated = true;
        for (String name : this.cubeNames) {
            CubeWrapper cube = this.cuberef.apply(name);

            float[] rot = this.interpolation.getInterpRot(cube, ci);
            float[] pos = this.interpolation.getInterpPos(cube, ci);

            Vector3f pr = cube.getPrevRotation();
            Vector3f pp = cube.getPrevRotationPoint();

            pr.x = rot[0];
            pr.y = rot[1];
            pr.z = rot[2];

            pp.x = pos[0];
            pp.y = pos[1];
            pp.z = pos[2];

        }
    }

    private PoseData getData() {
        return Objects.requireNonNull(this.poseStack.peek());
    }
}
