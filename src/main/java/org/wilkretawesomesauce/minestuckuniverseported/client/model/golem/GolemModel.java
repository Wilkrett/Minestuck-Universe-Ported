package org.wilkretawesomesauce.minestuckuniverseported.client.model.golem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.wilkretawesomesauce.minestuckuniverseported.entity.GolemEntity;

/**
 * Ported from ModularBosses (1.8)'s {@code client.models.entity.ModelGolem} - a 30-bone Tabula-style
 * "sculpture" model (giant 16x16x16 blocks standing in for shoulders/hips/limbs, one crude 16x16
 * texture stretched over the whole thing) driven entirely by hand-authored keyframe animation rather
 * than procedural swinging, matching {@link GolemEntity}'s own synced {@code aniID}/{@code aniFrame}
 * state machine (see that class's own doc comment). The keyframe arrays and per-state dispatch methods
 * below are a mechanical translation of the original's identical data (frame numbers, positions, and
 * rotations are untouched) - only the API surface changed: {@code ModelRenderer#rotationPointX/Y/Z}
 * and {@code #rotateAngleX/Y/Z} became {@link ModelPart}'s public {@code x/y/z}/{@code xRot/yRot/zRot}
 * fields, and {@code ModelUtils#moveParts} became {@link GolemKeyFrame#apply}.
 * <p>
 * Only {@code WAIST} and {@code HIP} are real top-level roots (matching the original's own
 * {@code render(...)}, which only ever renders those two) - every other bone nests underneath one of
 * them, see {@link #createBodyLayer()} for the exact hierarchy.
 */
public class GolemModel extends EntityModel<GolemEntity>
{
	public final ModelPart WAIST;
	public final ModelPart HIP;
	public final ModelPart Body;
	public final ModelPart RARM;
	public final ModelPart LARM;
	public final ModelPart LRShoulder2;
	public final ModelPart LFShoulder2;
	public final ModelPart RFShoulder2;
	public final ModelPart RRShoulder2;
	public final ModelPart RFShoulder1;
	public final ModelPart RRShoulder1;
	public final ModelPart LRShoulder1;
	public final ModelPart LFShoulder1;
	public final ModelPart RChest;
	public final ModelPart LChest;
	public final ModelPart HEAD;
	public final ModelPart RArm1;
	public final ModelPart RArm2;
	public final ModelPart LArm1;
	public final ModelPart LArm2;
	public final ModelPart LFHip;
	public final ModelPart LLEG;
	public final ModelPart RLEG;
	public final ModelPart RRHip;
	public final ModelPart LRHip;
	public final ModelPart RFHip;
	public final ModelPart LLeg1;
	public final ModelPart LLeg2;
	public final ModelPart RLeg1;
	public final ModelPart RLeg2;

	public GolemModel(ModelPart root)
	{
		super(RenderType::entityCutoutNoCull);

		this.WAIST = root.getChild("waist");
		this.Body = this.WAIST.getChild("body");
		this.RRShoulder1 = this.Body.getChild("rr_shoulder1");
		this.RRShoulder2 = this.Body.getChild("rr_shoulder2");
		this.RFShoulder1 = this.Body.getChild("rf_shoulder1");
		this.RFShoulder2 = this.Body.getChild("rf_shoulder2");
		this.LRShoulder1 = this.Body.getChild("lr_shoulder1");
		this.LRShoulder2 = this.Body.getChild("lr_shoulder2");
		this.LFShoulder1 = this.Body.getChild("lf_shoulder1");
		this.LFShoulder2 = this.Body.getChild("lf_shoulder2");
		this.HEAD = this.Body.getChild("head");
		this.RChest = this.Body.getChild("r_chest");
		this.LChest = this.Body.getChild("l_chest");
		this.RARM = this.Body.getChild("r_arm");
		this.RArm1 = this.RARM.getChild("r_arm1");
		this.RArm2 = this.RArm1.getChild("r_arm2");
		this.LARM = this.Body.getChild("l_arm");
		this.LArm1 = this.LARM.getChild("l_arm1");
		this.LArm2 = this.LArm1.getChild("l_arm2");

		this.HIP = root.getChild("hip");
		this.RLEG = this.HIP.getChild("r_leg");
		this.RLeg1 = this.RLEG.getChild("r_leg1");
		this.RLeg2 = this.RLeg1.getChild("r_leg2");
		this.LLEG = this.HIP.getChild("l_leg");
		this.LLeg1 = this.LLEG.getChild("l_leg1");
		this.LLeg2 = this.LLeg1.getChild("l_leg2");
		this.LRHip = this.HIP.getChild("lr_hip");
		this.RFHip = this.HIP.getChild("rf_hip");
		this.LFHip = this.HIP.getChild("lf_hip");
		this.RRHip = this.HIP.getChild("rr_hip");
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		CubeListBuilder pad = CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16);
		CubeListBuilder limb = CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, 0.0F, -8.0F, 16, 16, 16);
		CubeListBuilder waistBox = CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16, 16, 16);
		CubeListBuilder none = CubeListBuilder.create();

		// Real fix: WAIST isn't a pivot-only part like Body/HIP - the original's own constructor gives it
		// a real addBox(-8,-16,-8,16,16,16) torso cube (an earlier pass here only checked Body/HIP for a
		// zero-size box and wrongly assumed WAIST matched them too), and dropping it left a real hole in
		// the middle of the model - caught from a live screenshot.
		PartDefinition waist = root.addOrReplaceChild("waist", waistBox, PartPose.offset(0.0F, -25.0F, 0.0F));
		PartDefinition body = waist.addOrReplaceChild("body", none, PartPose.offsetAndRotation(0.0F, 14.0F, 0.0F, 0.0F, 1.5707963267948966F, 0.0F));

		body.addOrReplaceChild("rr_shoulder1", pad, PartPose.offsetAndRotation(-5.520000000000022F, -47.0F, -5.5F, 0.2617993877991494F, 0.0F, -0.2617993877991494F));
		body.addOrReplaceChild("rr_shoulder2", pad, PartPose.offsetAndRotation(-4.200000000000003F, -42.0F, -18.0F, 0.5235987755982988F, 0.0F, -0.2617993877991494F));
		body.addOrReplaceChild("rf_shoulder1", pad, PartPose.offsetAndRotation(5.5F, -47.0F, -5.5F, 0.2617993877991494F, 0.0F, 0.2617993877991494F));
		body.addOrReplaceChild("rf_shoulder2", pad, PartPose.offsetAndRotation(4.200000000000003F, -42.0F, -18.0F, 0.5235987755982988F, 0.0F, 0.2617993877991494F));
		body.addOrReplaceChild("lr_shoulder1", pad, PartPose.offsetAndRotation(-5.5F, -47.0F, 5.5F, -0.2617993877991494F, 0.0F, -0.2617993877991494F));
		body.addOrReplaceChild("lr_shoulder2", pad, PartPose.offsetAndRotation(-4.219999999999999F, -42.0F, 18.0F, -0.5235987755982988F, 0.0F, -0.2617993877991494F));
		body.addOrReplaceChild("lf_shoulder1", pad, PartPose.offsetAndRotation(5.520000000000002F, -47.0F, 5.5F, -0.2617993877991494F, 0.0F, 0.2617993877991494F));
		body.addOrReplaceChild("lf_shoulder2", pad, PartPose.offsetAndRotation(4.210000000000001F, -42.0F, 18.0F, -0.5235987755982988F, 0.0F, 0.2617993877991494F));
		body.addOrReplaceChild("head", pad, PartPose.offset(0.0F, -54.0F, 0.0F));
		body.addOrReplaceChild("r_chest", pad, PartPose.offset(0.0F, -38.0F, -8.0F));
		body.addOrReplaceChild("l_chest", pad, PartPose.offset(0.0F, -38.0F, 8.0F));

		PartDefinition rArm = body.addOrReplaceChild("r_arm", limb, PartPose.offsetAndRotation(0.0F, -40.0F, -21.0F, -0.6981317007977318F, 0.0F, 0.0F));
		PartDefinition rArm1 = rArm.addOrReplaceChild("r_arm1", limb, PartPose.offsetAndRotation(0.020000000000003126F, 12.0F, 0.00999999999999801F, 0.4363323129985824F, 0.0F, 0.0F));
		rArm1.addOrReplaceChild("r_arm2", limb, PartPose.offsetAndRotation(0.019999999999999574F, 12.0F, 0.01F, 0.17453292519943295F, 0.0F, 0.0F));

		PartDefinition lArm = body.addOrReplaceChild("l_arm", limb, PartPose.offsetAndRotation(0.0F, -40.0F, 21.0F, 0.6981317007977318F, 0.03490658503988659F, 0.0F));
		PartDefinition lArm1 = lArm.addOrReplaceChild("l_arm1", limb, PartPose.offsetAndRotation(0.020000000000003126F, 12.0F, 0.00999999999999801F, -0.4363323129985824F, 0.0F, 0.0F));
		lArm1.addOrReplaceChild("l_arm2", limb, PartPose.offsetAndRotation(0.020000000000003126F, 12.0F, 0.00999999999999801F, -0.17453292519943295F, 0.0F, 0.0F));

		PartDefinition hip = root.addOrReplaceChild("hip", none, PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, 0.0F, 1.5707963267948966F, 0.0F));

		PartDefinition rLeg = hip.addOrReplaceChild("r_leg", limb, PartPose.offsetAndRotation(0.0F, -11.0F, -7.0F, -0.3490658503988659F, 0.0F, 0.0F));
		PartDefinition rLeg1 = rLeg.addOrReplaceChild("r_leg1", limb, PartPose.offsetAndRotation(0.02F, 13.299999999999997F, -0.5F, 0.3490658503988659F, 0.0F, 0.0F));
		rLeg1.addOrReplaceChild("r_leg2", limb, PartPose.offset(0.00999999999999801F, 16.0F, 0.009999999999999787F));

		PartDefinition lLeg = hip.addOrReplaceChild("l_leg", limb, PartPose.offsetAndRotation(0.0F, -11.0F, 7.0F, 0.3490658503988659F, 0.0F, 0.0F));
		PartDefinition lLeg1 = lLeg.addOrReplaceChild("l_leg1", limb, PartPose.offsetAndRotation(0.019999999999999574F, 13.299999999999997F, 0.5F, -0.3490658503988659F, 0.0F, 0.0F));
		lLeg1.addOrReplaceChild("l_leg2", limb, PartPose.offset(0.010000000000001563F, 16.0F, 0.010000000000001563F));

		hip.addOrReplaceChild("lr_hip", pad, PartPose.offsetAndRotation(-5.520000000000003F, -9.0F, -5.5F, 0.2617993877991494F, 0.0F, -0.2617993877991494F));
		hip.addOrReplaceChild("rf_hip", pad, PartPose.offsetAndRotation(5.52F, -9.0F, 5.5F, -0.2617993877991494F, 0.0F, 0.2617993877991494F));
		hip.addOrReplaceChild("lf_hip", pad, PartPose.offsetAndRotation(5.5F, -9.0F, -5.5F, 0.2617993877991494F, 0.0F, 0.2617993877991494F));
		hip.addOrReplaceChild("rr_hip", pad, PartPose.offsetAndRotation(-5.5F, -9.0F, 5.5F, -0.2617993877991494F, 0.0F, -0.2617993877991494F));

		return LayerDefinition.create(mesh, 16, 16);
	}

	@Override
	public void setupAnim(GolemEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		float partialTick = ageInTicks - Mth.floor(ageInTicks);
		int frame = entity.aniFrame;

		switch(entity.aniID)
		{
			case GolemEntity.BUILD -> build(frame, partialTick);
			case GolemEntity.THROW -> throwAnim(frame, partialTick);
			case GolemEntity.ROLL -> roll(frame, partialTick);
			case GolemEntity.STOMP -> stomp(frame, partialTick);
			case GolemEntity.DIE -> die(frame, partialTick);
			default ->
			{
				stand(frame, partialTick);

				float speed = limbSwingAmount / 2F;
				float rAngle = Mth.cos(limbSwing / 2F) * (speed / 2F);
				float idle = Mth.cos((entity.tickCount + partialTick) / 20F) - 2.7F;

				this.LLEG.zRot = rAngle * 3F;
				this.RLEG.zRot = -rAngle * 3F;

				this.LLeg1.zRot = this.LLeg2.zRot = (rAngle <= 0) ? 0F : rAngle * 3F;
				this.RLeg1.zRot = this.RLeg2.zRot = (-rAngle <= 0) ? 0F : -rAngle * 3F;

				this.WAIST.y = -24F - (Mth.cos(limbSwing) * (speed * 5F));
				this.HIP.y = -9F - (Mth.cos(limbSwing) * (speed * 5F));

				this.LARM.zRot = -rAngle * 3F;
				this.RARM.zRot = rAngle * 3F;

				this.LArm1.zRot = this.LArm2.zRot = (-rAngle >= 0) ? 0F : -rAngle * 3F;
				this.RArm1.zRot = this.RArm2.zRot = (rAngle >= 0) ? 0F : rAngle * 3F;

				this.LARM.xRot = -idle / 6F;
				this.RARM.xRot = idle / 6F;

				this.LArm1.xRot = (idle + 1F) / 10F;
				this.RArm1.xRot = (-idle - 1F) / 10F;
			}
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
	{
		WAIST.render(poseStack, buffer, packedLight, packedOverlay, color);
		HIP.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	// ================================================================================================
	// Stand
	// ================================================================================================

	private static final GolemKeyFrame[] KF_Stand_HEAD = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LRShoulder2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LFShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RRShoulder2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RFShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LRShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LFShoulder2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RFShoulder2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RRShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LARM = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LArm1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LArm2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RARM = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RArm1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RArm2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RChest = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LChest = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_WAIST = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_HIP = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LFHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RRHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RFHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LRHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RLEG = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RLeg1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_RLeg2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LLEG = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LLeg1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stand_LLeg2 = new GolemKeyFrame[1];

	private static void buildStand()
	{
		KF_Stand_HEAD[0] = new GolemKeyFrame(0, 0, -54, 0, 0, 0, 0);
		KF_Stand_LRShoulder2[0] = new GolemKeyFrame(0, -4.22F, -42, 18, -30, 0, -15);
		KF_Stand_LFShoulder1[0] = new GolemKeyFrame(0, 5.52F, -47, 5.5F, -15, 0, 15);
		KF_Stand_RRShoulder2[0] = new GolemKeyFrame(0, -4.2F, -42, -18, 30, 0, -15);
		KF_Stand_RFShoulder1[0] = new GolemKeyFrame(0, 5.5F, -47, -5.5F, 15, 0, 15);
		KF_Stand_LRShoulder1[0] = new GolemKeyFrame(0, -5.5F, -47, 5.5F, -15, 0, -15);
		KF_Stand_LFShoulder2[0] = new GolemKeyFrame(0, 4.21F, -42, 18, -30, 0, 15);
		KF_Stand_RFShoulder2[0] = new GolemKeyFrame(0, 4.2F, -42, -18, 30, 0, 15);
		KF_Stand_RRShoulder1[0] = new GolemKeyFrame(0, -5.52F, -47, -5.5F, 15, 0, -15);
		KF_Stand_LARM[0] = new GolemKeyFrame(0, 0, -40, 21, 40, 0, 0);
		KF_Stand_LArm1[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, -25, 0, 0);
		KF_Stand_LArm2[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, -10, 0, 0);
		KF_Stand_RARM[0] = new GolemKeyFrame(0, 0, -40, -21, -40, 0, 0);
		KF_Stand_RArm1[0] = new GolemKeyFrame(0, 0.2F, 12, 0.01F, 25, 0, 0);
		KF_Stand_RArm2[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, 10, 0, 0);
		KF_Stand_RChest[0] = new GolemKeyFrame(0, 0, -38, -8, 0, 0, 0);
		KF_Stand_LChest[0] = new GolemKeyFrame(0, 0, -38, 8, 0, 0, 0);
		KF_Stand_WAIST[0] = new GolemKeyFrame(0, 0, -25, 0, 0, 0, 0);
		KF_Stand_HIP[0] = new GolemKeyFrame(0, 0, -9, 0, 0, 90, 0);
		KF_Stand_LFHip[0] = new GolemKeyFrame(0, 5.5F, -9, -5.5F, 15, 0, 15);
		KF_Stand_RRHip[0] = new GolemKeyFrame(0, -5.5F, -9, 5.5F, -15, 0, -15);
		KF_Stand_RFHip[0] = new GolemKeyFrame(0, 5.52F, -9, 5.5F, -15, 0, 15);
		KF_Stand_LRHip[0] = new GolemKeyFrame(0, -5.52F, -9, -5.5F, 15, 0, -15);
		KF_Stand_RLEG[0] = new GolemKeyFrame(0, 0, -11, -7, -20, 0, 0);
		KF_Stand_RLeg1[0] = new GolemKeyFrame(0, 0.02F, 13.3F, -0.5F, 20, 0, 0);
		KF_Stand_RLeg2[0] = new GolemKeyFrame(0, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Stand_LLEG[0] = new GolemKeyFrame(0, 0, -11, 7, 20, 0, 0);
		KF_Stand_LLeg1[0] = new GolemKeyFrame(0, 0.02F, 13.3F, 0.5F, -20, 0, 0);
		KF_Stand_LLeg2[0] = new GolemKeyFrame(0, 0.01F, 16, 0.01F, 0, 0, 0);
	}

	private void stand(int frame, float partialTick)
	{
		GolemKeyFrame.apply(frame, HIP, KF_Stand_HIP, partialTick);
		GolemKeyFrame.apply(frame, WAIST, KF_Stand_WAIST, partialTick);
		GolemKeyFrame.apply(frame, LFHip, KF_Stand_LFHip, partialTick);
		GolemKeyFrame.apply(frame, RChest, KF_Stand_RChest, partialTick);
		GolemKeyFrame.apply(frame, LRShoulder2, KF_Stand_LRShoulder2, partialTick);
		GolemKeyFrame.apply(frame, RArm1, KF_Stand_RArm1, partialTick);
		GolemKeyFrame.apply(frame, LFShoulder1, KF_Stand_LFShoulder1, partialTick);
		GolemKeyFrame.apply(frame, LArm2, KF_Stand_LArm2, partialTick);
		GolemKeyFrame.apply(frame, LARM, KF_Stand_LARM, partialTick);
		GolemKeyFrame.apply(frame, RLeg2, KF_Stand_RLeg2, partialTick);
		GolemKeyFrame.apply(frame, RRShoulder2, KF_Stand_RRShoulder2, partialTick);
		GolemKeyFrame.apply(frame, RFShoulder1, KF_Stand_RFShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RRHip, KF_Stand_RRHip, partialTick);
		GolemKeyFrame.apply(frame, LLEG, KF_Stand_LLEG, partialTick);
		GolemKeyFrame.apply(frame, LRShoulder1, KF_Stand_LRShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RLeg1, KF_Stand_RLeg1, partialTick);
		GolemKeyFrame.apply(frame, LArm1, KF_Stand_LArm1, partialTick);
		GolemKeyFrame.apply(frame, LFShoulder2, KF_Stand_LFShoulder2, partialTick);
		GolemKeyFrame.apply(frame, LLeg2, KF_Stand_LLeg2, partialTick);
		GolemKeyFrame.apply(frame, RArm2, KF_Stand_RArm2, partialTick);
		GolemKeyFrame.apply(frame, LChest, KF_Stand_LChest, partialTick);
		GolemKeyFrame.apply(frame, RFShoulder2, KF_Stand_RFShoulder2, partialTick);
		GolemKeyFrame.apply(frame, LLeg1, KF_Stand_LLeg1, partialTick);
		GolemKeyFrame.apply(frame, RFHip, KF_Stand_RFHip, partialTick);
		GolemKeyFrame.apply(frame, HEAD, KF_Stand_HEAD, partialTick);
		GolemKeyFrame.apply(frame, RRShoulder1, KF_Stand_RRShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RARM, KF_Stand_RARM, partialTick);
		GolemKeyFrame.apply(frame, RLEG, KF_Stand_RLEG, partialTick);
		GolemKeyFrame.apply(frame, LRHip, KF_Stand_LRHip, partialTick);
	}

	// ================================================================================================
	// Throw
	// ================================================================================================

	private static final GolemKeyFrame[] KF_Throw_HEAD = new GolemKeyFrame[6];
	private static final GolemKeyFrame[] KF_Throw_LRShoulder2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_LFShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_RRShoulder2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_RFShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_LRShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_LFShoulder2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_RFShoulder2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_RRShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_LARM = new GolemKeyFrame[6];
	private static final GolemKeyFrame[] KF_Throw_LArm1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_LArm2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Throw_RARM = new GolemKeyFrame[6];
	private static final GolemKeyFrame[] KF_Throw_RArm1 = new GolemKeyFrame[7];
	private static final GolemKeyFrame[] KF_Throw_RArm2 = new GolemKeyFrame[6];
	private static final GolemKeyFrame[] KF_Throw_RChest = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_LChest = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_WAIST = new GolemKeyFrame[7];
	private static final GolemKeyFrame[] KF_Throw_HIP = new GolemKeyFrame[7];
	private static final GolemKeyFrame[] KF_Throw_LFHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_RRHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_RFHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_LRHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Throw_RLEG = new GolemKeyFrame[6];
	private static final GolemKeyFrame[] KF_Throw_RLeg1 = new GolemKeyFrame[6];
	private static final GolemKeyFrame[] KF_Throw_RLeg2 = new GolemKeyFrame[7];
	private static final GolemKeyFrame[] KF_Throw_LLEG = new GolemKeyFrame[7];
	private static final GolemKeyFrame[] KF_Throw_LLeg1 = new GolemKeyFrame[7];
	private static final GolemKeyFrame[] KF_Throw_LLeg2 = new GolemKeyFrame[6];

	private static void buildThrow()
	{
		KF_Throw_HEAD[0] = new GolemKeyFrame(0, 0, -54, 0, 0, 0, 0);
		KF_Throw_HEAD[1] = new GolemKeyFrame(10, 0, -54, 0, 0, -50, 0);
		KF_Throw_HEAD[2] = new GolemKeyFrame(12, 0, -54, 0, 0, -50, 0);
		KF_Throw_HEAD[3] = new GolemKeyFrame(17, 0, -54, 0, 0, 40, 0);
		KF_Throw_HEAD[4] = new GolemKeyFrame(19, 0, -54, 0, 0, 40, 0);
		KF_Throw_HEAD[5] = new GolemKeyFrame(29, 0, -54, 0, 0, 0, 0);

		KF_Throw_LRShoulder2[0] = new GolemKeyFrame(0, -4.22F, -42, 18, -30, 0, -15);
		KF_Throw_LFShoulder1[0] = new GolemKeyFrame(0, 5.52F, -47, 5.5F, -15, 0, 15);
		KF_Throw_RRShoulder2[0] = new GolemKeyFrame(0, -4.2F, -42, -18, 30, 0, -15);
		KF_Throw_RFShoulder1[0] = new GolemKeyFrame(0, 5.5F, -47, -5.5F, 15, 0, 15);
		KF_Throw_LRShoulder1[0] = new GolemKeyFrame(0, -5.5F, -47, 5.5F, -15, 0, -15);
		KF_Throw_LFShoulder2[0] = new GolemKeyFrame(0, 4.21F, -42, 18, -30, 0, 15);
		KF_Throw_RFShoulder2[0] = new GolemKeyFrame(0, 4.2F, -42, -18, 30, 0, 15);
		KF_Throw_RRShoulder1[0] = new GolemKeyFrame(0, -5.52F, -47, -5.5F, 15, 0, -15);

		KF_Throw_LARM[0] = new GolemKeyFrame(0, 0, -40, 21, 40, 0, 0);
		KF_Throw_LARM[1] = new GolemKeyFrame(10, 0, -40, 21, 40, 0, -20);
		KF_Throw_LARM[2] = new GolemKeyFrame(12, 0, -40, 21, 40, 0, -20);
		KF_Throw_LARM[3] = new GolemKeyFrame(15, 0, -40, 21, 40, 0, 30);
		KF_Throw_LARM[4] = new GolemKeyFrame(19, 0, -40, 21, 40, 0, 30);
		KF_Throw_LARM[5] = new GolemKeyFrame(29, 0, -40, 21, 40, 0, 0);

		KF_Throw_LArm1[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, -25, 0, 0);

		KF_Throw_LArm2[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, -10, 0, 0);
		KF_Throw_LArm2[1] = new GolemKeyFrame(10, 0.02F, 12, 0.01F, -10, 0, -25);
		KF_Throw_LArm2[2] = new GolemKeyFrame(19, 0.02F, 12, 0.01F, -10, 0, -25);
		KF_Throw_LArm2[3] = new GolemKeyFrame(29, 0.02F, 12, 0.01F, -10, 0, 0);

		KF_Throw_RARM[0] = new GolemKeyFrame(0, 0, -40, -21, -40, 0, 0);
		KF_Throw_RARM[1] = new GolemKeyFrame(12, 0, -40, -21, -42, 2.61F, -180);
		KF_Throw_RARM[2] = new GolemKeyFrame(15, 0, -40, -21, -100, 0, -180);
		KF_Throw_RARM[3] = new GolemKeyFrame(17, 0, -40, -21, -80, 35, -155);
		KF_Throw_RARM[4] = new GolemKeyFrame(19, 0, -40, -21, -80, 35, -155);
		KF_Throw_RARM[5] = new GolemKeyFrame(29, 0, -40, -21, -40, 0, 0);

		KF_Throw_RArm1[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, 25, 0, 0);
		KF_Throw_RArm1[1] = new GolemKeyFrame(10, 0.02F, 12, 0.01F, -5.22F, 0, 10);
		KF_Throw_RArm1[2] = new GolemKeyFrame(12, 0.02F, 12, 0.01F, -5.22F, 0, 10);
		KF_Throw_RArm1[3] = new GolemKeyFrame(15, 0.02F, 12, 0.01F, 50, 0, 10);
		KF_Throw_RArm1[4] = new GolemKeyFrame(17, 0.02F, 12, 0.01F, 10, 0, 30);
		KF_Throw_RArm1[5] = new GolemKeyFrame(19, 0.02F, 12, 0.01F, 10, 0, 30);
		KF_Throw_RArm1[6] = new GolemKeyFrame(29, 0.02F, 12, 0.01F, 25, 0, 0);

		KF_Throw_RArm2[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, 10, 0, 0);
		KF_Throw_RArm2[1] = new GolemKeyFrame(12, 0.02F, 12, 0.01F, 10, 0, 0);
		KF_Throw_RArm2[2] = new GolemKeyFrame(15, 0.02F, 12, 0.01F, 35, 0, 0);
		KF_Throw_RArm2[3] = new GolemKeyFrame(17, 0.02F, 12, 0.01F, 15, 0, 0);
		KF_Throw_RArm2[4] = new GolemKeyFrame(19, 0.02F, 12, 0.01F, 15, 0, 0);
		KF_Throw_RArm2[5] = new GolemKeyFrame(29, 0.02F, 12, 0.01F, 10, 0, 0);

		KF_Throw_RChest[0] = new GolemKeyFrame(0, 0, -38, -8, 0, 0, 0);
		KF_Throw_LChest[0] = new GolemKeyFrame(0, 0, -38, 8, 0, 0, 0);

		KF_Throw_WAIST[0] = new GolemKeyFrame(0, 0, -25, 0, 0, 0, 0);
		KF_Throw_WAIST[1] = new GolemKeyFrame(10, 0, -22, 16, 0, 60, 0);
		KF_Throw_WAIST[2] = new GolemKeyFrame(12, 0, -22, 16, 0, 60, 0);
		KF_Throw_WAIST[3] = new GolemKeyFrame(15, 0, -24, -3.2F, 0, 0, 0);
		KF_Throw_WAIST[4] = new GolemKeyFrame(17, 0, -22, -16, 0, -40, 0);
		KF_Throw_WAIST[5] = new GolemKeyFrame(19, 0, -22, -16, 0, -40, 0);
		KF_Throw_WAIST[6] = new GolemKeyFrame(29, 0, -25, 0, 0, 0, 0);

		KF_Throw_HIP[0] = new GolemKeyFrame(0, 0, -9, 0, 0, 90, 0);
		KF_Throw_HIP[1] = new GolemKeyFrame(10, 0, -6, 16, 0, 150, 0);
		KF_Throw_HIP[2] = new GolemKeyFrame(12, 0, -6, 16, 0, 150, 0);
		KF_Throw_HIP[3] = new GolemKeyFrame(15, 0, -8, -3.2F, 0, 90, 0);
		KF_Throw_HIP[4] = new GolemKeyFrame(17, 0, -6, -16, 0, 50, 0);
		KF_Throw_HIP[5] = new GolemKeyFrame(19, 0, -6, -16, 0, 50, 0);
		KF_Throw_HIP[6] = new GolemKeyFrame(29, 0, -9, 0, 0, 90, 0);

		KF_Throw_LFHip[0] = new GolemKeyFrame(0, 5.5F, -9, -5.5F, 15, 0, 15);
		KF_Throw_RRHip[0] = new GolemKeyFrame(0, -5.5F, -9, 5.5F, -15, 0, -15);
		KF_Throw_RFHip[0] = new GolemKeyFrame(0, 5.52F, -9, 5.5F, -15, 0, 15);
		KF_Throw_LRHip[0] = new GolemKeyFrame(0, -5.52F, -9, -5.5F, 15, 0, -15);

		KF_Throw_RLEG[0] = new GolemKeyFrame(0, 0, -11, -7, -20, 0, 0);
		KF_Throw_RLEG[1] = new GolemKeyFrame(10, 0, -11, -7, -25, 0, 0);
		KF_Throw_RLEG[2] = new GolemKeyFrame(15, 0, -11, -7, -25, 0, 0);
		KF_Throw_RLEG[3] = new GolemKeyFrame(17, 0, -11, -7, -25, 25, -25);
		KF_Throw_RLEG[4] = new GolemKeyFrame(19, 0, -11, -7, -25, 25, -25);
		KF_Throw_RLEG[5] = new GolemKeyFrame(29, 0, -11, -7, -20, 0, 0);

		KF_Throw_RLeg1[0] = new GolemKeyFrame(0, 0.02F, 13.3F, -0.5F, 20, 0, 0);
		KF_Throw_RLeg1[1] = new GolemKeyFrame(10, 0.02F, 13.3F, -0.5F, 5, 5.5F, 0);
		KF_Throw_RLeg1[2] = new GolemKeyFrame(12, 0.02F, 13.3F, -0.5F, 5, 5.5F, 0);
		KF_Throw_RLeg1[3] = new GolemKeyFrame(15, 0.02F, 15.3F, -0.5F, 5, 5.5F, 0);
		KF_Throw_RLeg1[4] = new GolemKeyFrame(19, 0.02F, 15.3F, -0.5F, 5, 5.5F, 0);
		KF_Throw_RLeg1[5] = new GolemKeyFrame(29, 0.02F, 13.3F, -0.5F, 20, 0, 0);

		KF_Throw_RLeg2[0] = new GolemKeyFrame(0, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Throw_RLeg2[1] = new GolemKeyFrame(10, 0.01F, 14, 0.01F, 15, 0, 0);
		KF_Throw_RLeg2[2] = new GolemKeyFrame(12, 0.01F, 14, 0.01F, 15, 0, 0);
		KF_Throw_RLeg2[3] = new GolemKeyFrame(15, 0.01F, 11, 0.01F, 15, 0, 41.74F);
		KF_Throw_RLeg2[4] = new GolemKeyFrame(17, 0.01F, 12, 0.01F, 5, 0, 25);
		KF_Throw_RLeg2[5] = new GolemKeyFrame(19, 0.01F, 12, 0.01F, 5, 0, 25);
		KF_Throw_RLeg2[6] = new GolemKeyFrame(29, 0.01F, 16, 0.01F, 0, 0, 0);

		KF_Throw_LLEG[0] = new GolemKeyFrame(0, 0, -11, 7, 20, 0, 0);
		KF_Throw_LLEG[1] = new GolemKeyFrame(10, 0, -11, 7, 20, -60, -20);
		KF_Throw_LLEG[2] = new GolemKeyFrame(12, 0, -11, 7, 20, -60, -20);
		KF_Throw_LLEG[3] = new GolemKeyFrame(15, 0, -11, 7, 25, -75, -30);
		KF_Throw_LLEG[4] = new GolemKeyFrame(17, 0, -11, 7, 30, -15, -10);
		KF_Throw_LLEG[5] = new GolemKeyFrame(19, 0, -11, 7, 30, -15, -10);
		KF_Throw_LLEG[6] = new GolemKeyFrame(29, 0, -11, 7, 20, 0, 0);

		KF_Throw_LLeg1[0] = new GolemKeyFrame(0, 0.02F, 13.3F, 0.5F, -20, 0, 0);
		KF_Throw_LLeg1[1] = new GolemKeyFrame(10, 0.02F, 13.3F, 0.5F, -8, 10, -15);
		KF_Throw_LLeg1[2] = new GolemKeyFrame(12, 0.02F, 13.3F, 0.5F, -8, 10, -15);
		KF_Throw_LLeg1[3] = new GolemKeyFrame(15, 0.02F, 13.3F, 0.5F, 5, 0, 0);
		KF_Throw_LLeg1[4] = new GolemKeyFrame(17, 0.02F, 13.3F, 0.5F, 5, 0, 0);
		KF_Throw_LLeg1[5] = new GolemKeyFrame(19, 0.02F, 13.3F, 0.5F, 5, 0, 20);
		KF_Throw_LLeg1[6] = new GolemKeyFrame(29, 0.02F, 13.3F, 0.5F, -20, 0, 0);

		KF_Throw_LLeg2[0] = new GolemKeyFrame(0, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Throw_LLeg2[1] = new GolemKeyFrame(10, 0.01F, 13, 0.01F, 0, -15, 20);
		KF_Throw_LLeg2[2] = new GolemKeyFrame(12, 0.01F, 13, 0.01F, 0, -15, 20);
		KF_Throw_LLeg2[3] = new GolemKeyFrame(15, 0.01F, 14, 0.01F, 0, 0, 10);
		KF_Throw_LLeg2[4] = new GolemKeyFrame(19, 0.01F, 14, 0.01F, 0, 0, 10);
		KF_Throw_LLeg2[5] = new GolemKeyFrame(29, 0.01F, 16, 0.01F, 0, 0, 0);
	}

	private void throwAnim(int frame, float partialTick)
	{
		GolemKeyFrame.apply(frame, HIP, KF_Throw_HIP, partialTick);
		GolemKeyFrame.apply(frame, WAIST, KF_Throw_WAIST, partialTick);
		GolemKeyFrame.apply(frame, LFHip, KF_Throw_LFHip, partialTick);
		GolemKeyFrame.apply(frame, RChest, KF_Throw_RChest, partialTick);
		GolemKeyFrame.apply(frame, LRShoulder2, KF_Throw_LRShoulder2, partialTick);
		GolemKeyFrame.apply(frame, RArm1, KF_Throw_RArm1, partialTick);
		GolemKeyFrame.apply(frame, LFShoulder1, KF_Throw_LFShoulder1, partialTick);
		GolemKeyFrame.apply(frame, LArm2, KF_Throw_LArm2, partialTick);
		GolemKeyFrame.apply(frame, LARM, KF_Throw_LARM, partialTick);
		GolemKeyFrame.apply(frame, RLeg2, KF_Throw_RLeg2, partialTick);
		GolemKeyFrame.apply(frame, RRShoulder2, KF_Throw_RRShoulder2, partialTick);
		GolemKeyFrame.apply(frame, RFShoulder1, KF_Throw_RFShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RRHip, KF_Throw_RRHip, partialTick);
		GolemKeyFrame.apply(frame, LLEG, KF_Throw_LLEG, partialTick);
		GolemKeyFrame.apply(frame, LRShoulder1, KF_Throw_LRShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RLeg1, KF_Throw_RLeg1, partialTick);
		GolemKeyFrame.apply(frame, LArm1, KF_Throw_LArm1, partialTick);
		GolemKeyFrame.apply(frame, LFShoulder2, KF_Throw_LFShoulder2, partialTick);
		GolemKeyFrame.apply(frame, LLeg2, KF_Throw_LLeg2, partialTick);
		GolemKeyFrame.apply(frame, RArm2, KF_Throw_RArm2, partialTick);
		GolemKeyFrame.apply(frame, LChest, KF_Throw_LChest, partialTick);
		GolemKeyFrame.apply(frame, RFShoulder2, KF_Throw_RFShoulder2, partialTick);
		GolemKeyFrame.apply(frame, LLeg1, KF_Throw_LLeg1, partialTick);
		GolemKeyFrame.apply(frame, RFHip, KF_Throw_RFHip, partialTick);
		GolemKeyFrame.apply(frame, HEAD, KF_Throw_HEAD, partialTick);
		GolemKeyFrame.apply(frame, RRShoulder1, KF_Throw_RRShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RARM, KF_Throw_RARM, partialTick);
		GolemKeyFrame.apply(frame, RLEG, KF_Throw_RLEG, partialTick);
		GolemKeyFrame.apply(frame, LRHip, KF_Throw_LRHip, partialTick);
	}

	// ================================================================================================
	// Roll
	// ================================================================================================

	private static final GolemKeyFrame[] KF_Roll_HEAD = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_LRShoulder2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_LFShoulder1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_RRShoulder2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_RFShoulder1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_LRShoulder1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_LFShoulder2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_RFShoulder2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_RRShoulder1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_LARM = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_LArm1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_LArm2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_RARM = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_RArm1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_RArm2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_RChest = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_LChest = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_WAIST = new GolemKeyFrame[6];
	private static final GolemKeyFrame[] KF_Roll_HIP = new GolemKeyFrame[7];
	private static final GolemKeyFrame[] KF_Roll_LFHip = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_RRHip = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_RFHip = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_LRHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Roll_RLEG = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_RLeg1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_RLeg2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_LLEG = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_LLeg1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Roll_LLeg2 = new GolemKeyFrame[4];

	private static void buildRoll()
	{
		KF_Roll_HEAD[0] = new GolemKeyFrame(0, 0, -54, 0, 0, 0, 0);
		KF_Roll_HEAD[1] = new GolemKeyFrame(3, 6, -26, 9, -5, -30, 40);
		KF_Roll_HEAD[2] = new GolemKeyFrame(20, 6, -26, 9, -5, -30, 40);
		KF_Roll_HEAD[3] = new GolemKeyFrame(23, 0, -54, 0, 0, 0, 0);

		KF_Roll_LRShoulder2[0] = new GolemKeyFrame(0, -4.22F, -42, 18, -30, 0, -15);
		KF_Roll_LRShoulder2[1] = new GolemKeyFrame(3, -13.22F, -10, 17, -7, 29, 0);
		KF_Roll_LRShoulder2[2] = new GolemKeyFrame(20, -13.22F, -10, 17, -7, 29, 0);
		KF_Roll_LRShoulder2[3] = new GolemKeyFrame(23, -4.22F, -42, 18, -30, 0, -15);

		KF_Roll_LFShoulder1[0] = new GolemKeyFrame(0, 5.52F, -47, 5.5F, -15, 0, 15);
		KF_Roll_LFShoulder1[1] = new GolemKeyFrame(3, 19.52F, -15, 0.5F, -7, -6, -4);
		KF_Roll_LFShoulder1[2] = new GolemKeyFrame(20, 19.52F, -15, 0.5F, -7, -6, -4);
		KF_Roll_LFShoulder1[3] = new GolemKeyFrame(23, 5.52F, -47, 5.5F, -15, 0, 15);

		KF_Roll_RRShoulder2[0] = new GolemKeyFrame(0, -4.2F, -42, -18, 30, 0, -15);
		KF_Roll_RRShoulder2[1] = new GolemKeyFrame(3, -9.2F, -21, -18, 55, 0, -39);
		KF_Roll_RRShoulder2[2] = new GolemKeyFrame(20, -9.2F, -21, -18, 55, 0, -39);
		KF_Roll_RRShoulder2[3] = new GolemKeyFrame(23, -4.2F, -42, -18, 30, 0, -15);

		KF_Roll_RFShoulder1[0] = new GolemKeyFrame(0, 5.5F, -47, -5.5F, 15, 0, 15);
		KF_Roll_RFShoulder1[1] = new GolemKeyFrame(3, 5.5F, -35, -5.5F, 15, 0, 15);
		KF_Roll_RFShoulder1[2] = new GolemKeyFrame(20, 5.5F, -35, -5.5F, 15, 0, 15);
		KF_Roll_RFShoulder1[3] = new GolemKeyFrame(23, 5.5F, -47, -5.5F, 15, 0, 15);

		KF_Roll_LRShoulder1[0] = new GolemKeyFrame(0, -5.5F, -47, 5.5F, -15, 0, -15);
		KF_Roll_LRShoulder1[1] = new GolemKeyFrame(3, -5.5F, -34, 5.5F, -15, 0, -15);
		KF_Roll_LRShoulder1[2] = new GolemKeyFrame(20, -5.5F, -34, 5.5F, -15, 0, -15);
		KF_Roll_LRShoulder1[3] = new GolemKeyFrame(23, -5.5F, -47, 5.5F, -15, 0, -15);

		KF_Roll_LFShoulder2[0] = new GolemKeyFrame(0, 4.21F, -42, 18, -30, 0, 15);
		KF_Roll_LFShoulder2[1] = new GolemKeyFrame(3, 12.21F, -9, 17, -1, -32, 30);
		KF_Roll_LFShoulder2[2] = new GolemKeyFrame(20, 12.21F, -9, 17, -1, -32, 30);
		KF_Roll_LFShoulder2[3] = new GolemKeyFrame(23, 4.21F, -42, 18, -30, 0, 15);

		KF_Roll_RFShoulder2[0] = new GolemKeyFrame(0, 4.2F, -42, -18, 30, 0, 15);
		KF_Roll_RFShoulder2[1] = new GolemKeyFrame(3, 10.2F, -25, -17, 64, -20, 6);
		KF_Roll_RFShoulder2[2] = new GolemKeyFrame(20, 10.2F, -25, -17, 64, -20, 6);
		KF_Roll_RFShoulder2[3] = new GolemKeyFrame(23, 4.2F, -42, -18, 30, 0, 15);

		KF_Roll_RRShoulder1[0] = new GolemKeyFrame(0, -5.52F, -47, -5.5F, 15, 0, -15);
		KF_Roll_RRShoulder1[1] = new GolemKeyFrame(3, -15.52F, -28, -2.5F, 13, -7, -56);
		KF_Roll_RRShoulder1[2] = new GolemKeyFrame(20, -15.52F, -28, -2.5F, 13, -7, -56);
		KF_Roll_RRShoulder1[3] = new GolemKeyFrame(23, -5.52F, -47, -5.5F, 15, 0, -15);

		KF_Roll_LARM[0] = new GolemKeyFrame(0, 0, -40, 21, 40, 0, 0);
		KF_Roll_LARM[1] = new GolemKeyFrame(3, -1, -33, 18, 7, 0, 0);
		KF_Roll_LARM[2] = new GolemKeyFrame(20, -1, -33, 18, 7, 0, 0);
		KF_Roll_LARM[3] = new GolemKeyFrame(23, 0, -40, 21, 40, 0, 0);

		KF_Roll_LArm1[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, -25, 0, 0);
		KF_Roll_LArm1[1] = new GolemKeyFrame(3, 17.02F, 1, 0.01F, -12, 54, 0);
		KF_Roll_LArm1[2] = new GolemKeyFrame(20, 17.02F, 1, 0.01F, -12, 54, 0);
		KF_Roll_LArm1[3] = new GolemKeyFrame(23, 0.02F, 12, 0.01F, -25, 0, 0);

		KF_Roll_LArm2[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, -10, 0, 0);
		KF_Roll_LArm2[1] = new GolemKeyFrame(3, -11.98F, 18, -5.99F, -12, -38, 0);
		KF_Roll_LArm2[2] = new GolemKeyFrame(20, -11.98F, 18, -5.99F, -12, -38, 0);
		KF_Roll_LArm2[3] = new GolemKeyFrame(23, 0.02F, 12, 0.01F, -10, 0, 0);

		KF_Roll_RARM[0] = new GolemKeyFrame(0, 0, -40, -21, -40, 0, 0);
		KF_Roll_RARM[1] = new GolemKeyFrame(3, -5, -8, -10, -68, 54, 0);
		KF_Roll_RARM[2] = new GolemKeyFrame(20, -5, -8, -10, -68, 54, 0);
		KF_Roll_RARM[3] = new GolemKeyFrame(23, 0, -40, -21, -40, 0, 0);

		KF_Roll_RArm1[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, 25, 0, 0);
		KF_Roll_RArm1[1] = new GolemKeyFrame(3, 11.02F, -7, -2.99F, 2, 0, -32);
		KF_Roll_RArm1[2] = new GolemKeyFrame(20, 11.02F, -7, -2.99F, 2, 0, -32);
		KF_Roll_RArm1[3] = new GolemKeyFrame(23, 0.02F, 12, 0.01F, 25, 0, 0);

		KF_Roll_RArm2[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, 10, 0, 0);
		KF_Roll_RArm2[1] = new GolemKeyFrame(3, -10.98F, -5, 15.01F, 14, -6, -15);
		KF_Roll_RArm2[2] = new GolemKeyFrame(20, -10.98F, -5, 15.01F, 14, -6, -15);
		KF_Roll_RArm2[3] = new GolemKeyFrame(23, 0.02F, 12, 0.01F, 10, 0, 0);

		KF_Roll_RChest[0] = new GolemKeyFrame(0, 0, -38, -8, 0, 0, 0);
		KF_Roll_RChest[1] = new GolemKeyFrame(3, 19, -11, -12, 35, -53, -26);
		KF_Roll_RChest[2] = new GolemKeyFrame(20, 19, -11, -12, 35, -53, -26);
		KF_Roll_RChest[3] = new GolemKeyFrame(23, 0, -38, -8, 0, 0, 0);

		KF_Roll_LChest[0] = new GolemKeyFrame(0, 0, -38, 8, 0, 0, 0);
		KF_Roll_LChest[1] = new GolemKeyFrame(3, -11, -25, 11, 0, 26, 16);
		KF_Roll_LChest[2] = new GolemKeyFrame(20, -11, -25, 11, 0, 26, 16);
		KF_Roll_LChest[3] = new GolemKeyFrame(23, 0, -38, 8, 0, 0, 0);

		KF_Roll_LFHip[0] = new GolemKeyFrame(0, 5.5F, -9, -5.5F, 15, 0, 15);
		KF_Roll_LFHip[1] = new GolemKeyFrame(3, 13.5F, 8, -2.5F, 3, 6, 33);
		KF_Roll_LFHip[2] = new GolemKeyFrame(20, 13.5F, 8, -2.5F, 3, 6, 33);
		KF_Roll_LFHip[3] = new GolemKeyFrame(23, 5.5F, -9, -5.5F, 15, 0, 15);

		KF_Roll_RRHip[0] = new GolemKeyFrame(0, -5.5F, -9, 5.5F, -15, 0, -15);
		KF_Roll_RRHip[1] = new GolemKeyFrame(3, -0.5F, 3, 18.5F, 7, 7, 2);
		KF_Roll_RRHip[2] = new GolemKeyFrame(20, -0.5F, 3, 18.5F, 7, 7, 2);
		KF_Roll_RRHip[3] = new GolemKeyFrame(23, -5.5F, -9, 5.5F, -15, 0, -15);

		KF_Roll_RFHip[0] = new GolemKeyFrame(0, 5.52F, -9, 5.5F, -15, 0, 15);
		KF_Roll_RFHip[1] = new GolemKeyFrame(3, 10.52F, 18, 12.5F, -2, -36, 35);
		KF_Roll_RFHip[2] = new GolemKeyFrame(20, 10.52F, 18, 12.5F, -2, -36, 35);
		KF_Roll_RFHip[3] = new GolemKeyFrame(23, 5.52F, -9, 5.5F, -15, 0, 15);

		KF_Roll_LRHip[0] = new GolemKeyFrame(0, -5.52F, -9, -5.5F, 15, 0, -15);

		KF_Roll_RLEG[0] = new GolemKeyFrame(0, 0, -11, -7, -20, 0, 0);
		KF_Roll_RLEG[1] = new GolemKeyFrame(3, 0, 2, -7, -41, 0, 0);
		KF_Roll_RLEG[2] = new GolemKeyFrame(20, 0, 2, -7, -41, 0, 0);
		KF_Roll_RLEG[3] = new GolemKeyFrame(23, 0, -11, -7, -20, 0, 0);

		KF_Roll_RLeg1[0] = new GolemKeyFrame(0, 0.02F, 13.3F, -0.5F, 20, 0, 0);
		KF_Roll_RLeg1[1] = new GolemKeyFrame(3, 0.02F, 13.3F, -0.5F, 51, -13, 0);
		KF_Roll_RLeg1[2] = new GolemKeyFrame(20, 0.02F, 13.3F, -0.5F, 51, -13, 0);
		KF_Roll_RLeg1[3] = new GolemKeyFrame(23, 0.02F, 13.3F, -0.5F, 20, 0, 0);

		KF_Roll_RLeg2[0] = new GolemKeyFrame(0, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Roll_RLeg2[1] = new GolemKeyFrame(3, 12.01F, 9, 0.01F, 81, 1, -12);
		KF_Roll_RLeg2[2] = new GolemKeyFrame(20, 12.01F, 9, 0.01F, 81, 1, -12);
		KF_Roll_RLeg2[3] = new GolemKeyFrame(23, 0.01F, 16, 0.01F, 0, 0, 0);

		KF_Roll_LLEG[0] = new GolemKeyFrame(0, 0, -11, 7, 20, 0, 0);
		KF_Roll_LLEG[1] = new GolemKeyFrame(3, 0, 2, 7, 20, 0, 0);
		KF_Roll_LLEG[2] = new GolemKeyFrame(20, 0, 2, 7, 20, 0, 0);
		KF_Roll_LLEG[3] = new GolemKeyFrame(23, 0, -11, 7, 20, 0, 0);

		KF_Roll_LLeg1[0] = new GolemKeyFrame(0, 0.02F, 13.3F, 0.5F, -20, 0, 0);
		KF_Roll_LLeg1[1] = new GolemKeyFrame(3, 1.02F, 7.3F, 10.5F, -37, 5, 0);
		KF_Roll_LLeg1[2] = new GolemKeyFrame(20, 1.02F, 7.3F, 10.5F, -37, 5, 0);
		KF_Roll_LLeg1[3] = new GolemKeyFrame(23, 0.02F, 13.3F, 0.5F, -20, 0, 0);

		KF_Roll_LLeg2[0] = new GolemKeyFrame(0, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Roll_LLeg2[1] = new GolemKeyFrame(3, 0.01F, 15, 0.01F, -51, 0, 0);
		KF_Roll_LLeg2[2] = new GolemKeyFrame(20, 0.01F, 15, 0.01F, -51, 0, 0);
		KF_Roll_LLeg2[3] = new GolemKeyFrame(23, 0.01F, 16, 0.01F, 0, 0, 0);

		KF_Roll_WAIST[0] = new GolemKeyFrame(0, 0, -25, 0, 0, 0, 0);
		KF_Roll_WAIST[1] = new GolemKeyFrame(4, 0, -25, 0, 0, 0, 0);
		KF_Roll_WAIST[2] = new GolemKeyFrame(7, 0, -7, 0, 0, 0, 0);
		KF_Roll_WAIST[3] = new GolemKeyFrame(10, 0, -7, 0, 0, 0, 0);
		KF_Roll_WAIST[4] = new GolemKeyFrame(20, 0, -7, 0, 360, 0, 0);
		KF_Roll_WAIST[5] = new GolemKeyFrame(23, 0, -25, 0, 0, 0, 0);

		KF_Roll_HIP[0] = new GolemKeyFrame(0, 0, -9, 0, 0, 90, 0);
		KF_Roll_HIP[1] = new GolemKeyFrame(3, 0, -25, 0, 0, 0, 0);
		KF_Roll_HIP[2] = new GolemKeyFrame(4, 0, -25, 0, 0, 0, 0);
		KF_Roll_HIP[3] = new GolemKeyFrame(7, 0, -7, 0, 0, 0, 0);
		KF_Roll_HIP[4] = new GolemKeyFrame(10, 0, -7, 0, 0, 0, 0);
		KF_Roll_HIP[5] = new GolemKeyFrame(20, 0, -7, 0, 360, 0, 0);
		KF_Roll_HIP[6] = new GolemKeyFrame(23, 0, -9, 0, 0, 90, 0);
	}

	private void roll(int frame, float partialTick)
	{
		GolemKeyFrame.apply(frame, HIP, KF_Roll_HIP, partialTick);
		GolemKeyFrame.apply(frame, WAIST, KF_Roll_WAIST, partialTick);
		GolemKeyFrame.apply(frame, LFHip, KF_Roll_LFHip, partialTick);
		GolemKeyFrame.apply(frame, RChest, KF_Roll_RChest, partialTick);
		GolemKeyFrame.apply(frame, LRShoulder2, KF_Roll_LRShoulder2, partialTick);
		GolemKeyFrame.apply(frame, RArm1, KF_Roll_RArm1, partialTick);
		GolemKeyFrame.apply(frame, LFShoulder1, KF_Roll_LFShoulder1, partialTick);
		GolemKeyFrame.apply(frame, LArm2, KF_Roll_LArm2, partialTick);
		GolemKeyFrame.apply(frame, LARM, KF_Roll_LARM, partialTick);
		GolemKeyFrame.apply(frame, RLeg2, KF_Roll_RLeg2, partialTick);
		GolemKeyFrame.apply(frame, RRShoulder2, KF_Roll_RRShoulder2, partialTick);
		GolemKeyFrame.apply(frame, RFShoulder1, KF_Roll_RFShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RRHip, KF_Roll_RRHip, partialTick);
		GolemKeyFrame.apply(frame, LLEG, KF_Roll_LLEG, partialTick);
		GolemKeyFrame.apply(frame, LRShoulder1, KF_Roll_LRShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RLeg1, KF_Roll_RLeg1, partialTick);
		GolemKeyFrame.apply(frame, LArm1, KF_Roll_LArm1, partialTick);
		GolemKeyFrame.apply(frame, LFShoulder2, KF_Roll_LFShoulder2, partialTick);
		GolemKeyFrame.apply(frame, LLeg2, KF_Roll_LLeg2, partialTick);
		GolemKeyFrame.apply(frame, RArm2, KF_Roll_RArm2, partialTick);
		GolemKeyFrame.apply(frame, LChest, KF_Roll_LChest, partialTick);
		GolemKeyFrame.apply(frame, RFShoulder2, KF_Roll_RFShoulder2, partialTick);
		GolemKeyFrame.apply(frame, LLeg1, KF_Roll_LLeg1, partialTick);
		GolemKeyFrame.apply(frame, RFHip, KF_Roll_RFHip, partialTick);
		GolemKeyFrame.apply(frame, HEAD, KF_Roll_HEAD, partialTick);
		GolemKeyFrame.apply(frame, RRShoulder1, KF_Roll_RRShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RARM, KF_Roll_RARM, partialTick);
		GolemKeyFrame.apply(frame, RLEG, KF_Roll_RLEG, partialTick);
		GolemKeyFrame.apply(frame, LRHip, KF_Roll_LRHip, partialTick);
	}

	// ================================================================================================
	// Stomp
	// ================================================================================================

	private static final GolemKeyFrame[] KF_Stomp_HEAD = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_LRShoulder2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_LFShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_RRShoulder2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_RFShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_LRShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_LFShoulder2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_RFShoulder2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_RRShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_LARM = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Stomp_LArm1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_LArm2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_RARM = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Stomp_RArm1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_RArm2 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_RChest = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_LChest = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_WAIST = new GolemKeyFrame[6];
	private static final GolemKeyFrame[] KF_Stomp_HIP = new GolemKeyFrame[5];
	private static final GolemKeyFrame[] KF_Stomp_LFHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_RRHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_RFHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_LRHip = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Stomp_RLEG = new GolemKeyFrame[6];
	private static final GolemKeyFrame[] KF_Stomp_RLeg1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Stomp_RLeg2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Stomp_LLEG = new GolemKeyFrame[5];
	private static final GolemKeyFrame[] KF_Stomp_LLeg1 = new GolemKeyFrame[5];
	private static final GolemKeyFrame[] KF_Stomp_LLeg2 = new GolemKeyFrame[5];

	private static void buildStomp()
	{
		KF_Stomp_HEAD[0] = new GolemKeyFrame(0, 0, -54, 0, 0, 0, 0);
		KF_Stomp_LRShoulder2[0] = new GolemKeyFrame(0, -4.22F, -42, 18, -30, 0, -15);
		KF_Stomp_LFShoulder1[0] = new GolemKeyFrame(0, 5.52F, -47, 5.5F, -15, 0, 15);
		KF_Stomp_RRShoulder2[0] = new GolemKeyFrame(0, -4.2F, -42, -18, 30, 0, -15);
		KF_Stomp_RFShoulder1[0] = new GolemKeyFrame(0, 5.5F, -47, -5.5F, 15, 0, 15);
		KF_Stomp_LRShoulder1[0] = new GolemKeyFrame(0, -5.5F, -47, 5.5F, -15, 0, -15);
		KF_Stomp_LFShoulder2[0] = new GolemKeyFrame(0, 4.21F, -42, 18, -30, 0, 15);
		KF_Stomp_RFShoulder2[0] = new GolemKeyFrame(0, 4.2F, -42, -18, 30, 0, 15);
		KF_Stomp_RRShoulder1[0] = new GolemKeyFrame(0, -5.52F, -47, -5.5F, 15, 0, -15);

		KF_Stomp_LARM[0] = new GolemKeyFrame(0, 0, -40, 21, 40, 0, 0);
		KF_Stomp_LARM[1] = new GolemKeyFrame(5, 0, -40, 21, 55, 0, 0);
		KF_Stomp_LARM[2] = new GolemKeyFrame(12, 0, -40, 21, 55, 0, 0);
		KF_Stomp_LARM[3] = new GolemKeyFrame(17, 0, -40, 21, 40, 0, 0);

		KF_Stomp_LArm1[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, -25, 0, 0);
		KF_Stomp_LArm2[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, -10, 0, 0);

		KF_Stomp_RARM[0] = new GolemKeyFrame(0, 0, -40, -21, -40, 0, 0);
		KF_Stomp_RARM[1] = new GolemKeyFrame(5, 0, -40, -21, -55, 0, 0);
		KF_Stomp_RARM[2] = new GolemKeyFrame(12, 0, -40, -21, -55, 0, 0);
		KF_Stomp_RARM[3] = new GolemKeyFrame(17, 0, -40, -21, -40, 0, 0);

		KF_Stomp_RArm1[0] = new GolemKeyFrame(0, 0.2F, 12, 0.01F, 25, 0, 0);
		KF_Stomp_RArm2[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, 10, 0, 0);
		KF_Stomp_RChest[0] = new GolemKeyFrame(0, 0, -38, -8, 0, 0, 0);
		KF_Stomp_LChest[0] = new GolemKeyFrame(0, 0, -38, 8, 0, 0, 0);

		KF_Stomp_LFHip[0] = new GolemKeyFrame(0, 5.5F, -9, -5.5F, 15, 0, 15);
		KF_Stomp_RRHip[0] = new GolemKeyFrame(0, -5.5F, -9, 5.5F, -15, 0, -15);
		KF_Stomp_RFHip[0] = new GolemKeyFrame(0, 5.52F, -9, 5.5F, -15, 0, 15);
		KF_Stomp_LRHip[0] = new GolemKeyFrame(0, -5.52F, -9, -5.5F, 15, 0, -15);

		KF_Stomp_RLEG[0] = new GolemKeyFrame(0, 0, -11, -7, -20, 0, 0);
		KF_Stomp_RLEG[1] = new GolemKeyFrame(5, 3, -8, -7, -20, 0, -65);
		KF_Stomp_RLEG[2] = new GolemKeyFrame(7, 3, -8, -7, -20, 0, -65);
		KF_Stomp_RLEG[3] = new GolemKeyFrame(10, 3, -5, -7, -20, 0, -53);
		KF_Stomp_RLEG[4] = new GolemKeyFrame(12, 3, -5, -7, -20, 0, -53);
		KF_Stomp_RLEG[5] = new GolemKeyFrame(17, 0, -11, -7, -20, 0, 0);

		KF_Stomp_RLeg1[0] = new GolemKeyFrame(0, 0.02F, 13.3F, -0.5F, 20, 0, 0);
		KF_Stomp_RLeg1[1] = new GolemKeyFrame(5, 0.02F, 11.3F, -0.05F, 20, 0, 15);
		KF_Stomp_RLeg1[2] = new GolemKeyFrame(12, 0.02F, 11.3F, -0.05F, 20, 0, 15);
		KF_Stomp_RLeg1[3] = new GolemKeyFrame(17, 0.02F, 13.3F, -0.5F, 20, 0, 0);

		KF_Stomp_RLeg2[0] = new GolemKeyFrame(0, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Stomp_RLeg2[1] = new GolemKeyFrame(5, 2.01F, 11, 0.01F, 0, 0, 40);
		KF_Stomp_RLeg2[2] = new GolemKeyFrame(12, 2.01F, 11, 0.01F, 0, 0, 40);
		KF_Stomp_RLeg2[3] = new GolemKeyFrame(17, 0.01F, 16, 0.01F, 0, 0, 0);

		KF_Stomp_LLEG[0] = new GolemKeyFrame(0, 0, -10, 7, 20, 0, 0);
		KF_Stomp_LLEG[1] = new GolemKeyFrame(7, 0, -10, 7, 20, 0, 0);
		KF_Stomp_LLEG[2] = new GolemKeyFrame(10, 0, -11, 7, 20, 0, 18);
		KF_Stomp_LLEG[3] = new GolemKeyFrame(12, 0, -11, 7, 20, 0, 18);
		KF_Stomp_LLEG[4] = new GolemKeyFrame(17, 0, -10, 7, 20, 0, 0);

		KF_Stomp_LLeg1[0] = new GolemKeyFrame(0, 0.02F, 13.3F, 0.5F, -20, 0, 0);
		KF_Stomp_LLeg1[1] = new GolemKeyFrame(7, 0.02F, 13.3F, 0.5F, -20, 0, 0);
		KF_Stomp_LLeg1[2] = new GolemKeyFrame(10, 1.02F, 12.3F, 0.5F, -20, 0, 18);
		KF_Stomp_LLeg1[3] = new GolemKeyFrame(12, 1.02F, 12.3F, 0.5F, -20, 0, 18);
		KF_Stomp_LLeg1[4] = new GolemKeyFrame(17, 0.02F, 13.3F, 0.5F, -20, 0, 0);

		KF_Stomp_LLeg2[0] = new GolemKeyFrame(0, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Stomp_LLeg2[1] = new GolemKeyFrame(7, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Stomp_LLeg2[2] = new GolemKeyFrame(10, 1.01F, 13, 0.01F, 0, 0, 0);
		KF_Stomp_LLeg2[3] = new GolemKeyFrame(12, 1.01F, 13, 0.01F, 0, 0, 0);
		KF_Stomp_LLeg2[4] = new GolemKeyFrame(17, 0.01F, 16, 0.01F, 0, 0, 0);

		KF_Stomp_WAIST[0] = new GolemKeyFrame(0, 0, -25, 0, 0, 0, 0);
		KF_Stomp_WAIST[1] = new GolemKeyFrame(5, 0, -25, 0, 10, 0, 0);
		KF_Stomp_WAIST[2] = new GolemKeyFrame(7, 0, -25, 0, 10, 0, 0);
		KF_Stomp_WAIST[3] = new GolemKeyFrame(10, 0, -16, -10, 10, 0, 0);
		KF_Stomp_WAIST[4] = new GolemKeyFrame(12, 0, -16, -10, 10, 0, 0);
		KF_Stomp_WAIST[5] = new GolemKeyFrame(17, 0, -25, 0, 0, 0, 0);

		KF_Stomp_HIP[0] = new GolemKeyFrame(0, 0, -9, 0, 0, 90, 0);
		KF_Stomp_HIP[1] = new GolemKeyFrame(7, 0, -9, 0, 0, 90, 0);
		KF_Stomp_HIP[2] = new GolemKeyFrame(10, 0, 0, -10, 0, 90, 0);
		KF_Stomp_HIP[3] = new GolemKeyFrame(12, 0, 0, -10, 0, 90, 0);
		KF_Stomp_HIP[4] = new GolemKeyFrame(17, 0, -9, 0, 0, 90, 0);
	}

	private void stomp(int frame, float partialTick)
	{
		GolemKeyFrame.apply(frame, HIP, KF_Stomp_HIP, partialTick);
		GolemKeyFrame.apply(frame, WAIST, KF_Stomp_WAIST, partialTick);
		GolemKeyFrame.apply(frame, LFHip, KF_Stomp_LFHip, partialTick);
		GolemKeyFrame.apply(frame, RChest, KF_Stomp_RChest, partialTick);
		GolemKeyFrame.apply(frame, LRShoulder2, KF_Stomp_LRShoulder2, partialTick);
		GolemKeyFrame.apply(frame, RArm1, KF_Stomp_RArm1, partialTick);
		GolemKeyFrame.apply(frame, LFShoulder1, KF_Stomp_LFShoulder1, partialTick);
		GolemKeyFrame.apply(frame, LArm2, KF_Stomp_LArm2, partialTick);
		GolemKeyFrame.apply(frame, LARM, KF_Stomp_LARM, partialTick);
		GolemKeyFrame.apply(frame, RLeg2, KF_Stomp_RLeg2, partialTick);
		GolemKeyFrame.apply(frame, RRShoulder2, KF_Stomp_RRShoulder2, partialTick);
		GolemKeyFrame.apply(frame, RFShoulder1, KF_Stomp_RFShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RRHip, KF_Stomp_RRHip, partialTick);
		GolemKeyFrame.apply(frame, LLEG, KF_Stomp_LLEG, partialTick);
		GolemKeyFrame.apply(frame, LRShoulder1, KF_Stomp_LRShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RLeg1, KF_Stomp_RLeg1, partialTick);
		GolemKeyFrame.apply(frame, LArm1, KF_Stomp_LArm1, partialTick);
		GolemKeyFrame.apply(frame, LFShoulder2, KF_Stomp_LFShoulder2, partialTick);
		GolemKeyFrame.apply(frame, LLeg2, KF_Stomp_LLeg2, partialTick);
		GolemKeyFrame.apply(frame, RArm2, KF_Stomp_RArm2, partialTick);
		GolemKeyFrame.apply(frame, LChest, KF_Stomp_LChest, partialTick);
		GolemKeyFrame.apply(frame, RFShoulder2, KF_Stomp_RFShoulder2, partialTick);
		GolemKeyFrame.apply(frame, LLeg1, KF_Stomp_LLeg1, partialTick);
		GolemKeyFrame.apply(frame, RFHip, KF_Stomp_RFHip, partialTick);
		GolemKeyFrame.apply(frame, HEAD, KF_Stomp_HEAD, partialTick);
		GolemKeyFrame.apply(frame, RRShoulder1, KF_Stomp_RRShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RARM, KF_Stomp_RARM, partialTick);
		GolemKeyFrame.apply(frame, RLEG, KF_Stomp_RLEG, partialTick);
		GolemKeyFrame.apply(frame, LRHip, KF_Stomp_LRHip, partialTick);
	}

	// ================================================================================================
	// Die
	// ================================================================================================

	private static final GolemKeyFrame[] KF_Die_HEAD = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Die_LRShoulder2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Die_LFShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Die_RRShoulder2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Die_RFShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Die_LRShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Die_LFShoulder2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Die_RFShoulder2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Die_RRShoulder1 = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Die_LARM = new GolemKeyFrame[6];
	private static final GolemKeyFrame[] KF_Die_LArm1 = new GolemKeyFrame[5];
	private static final GolemKeyFrame[] KF_Die_LArm2 = new GolemKeyFrame[6];
	private static final GolemKeyFrame[] KF_Die_RARM = new GolemKeyFrame[6];
	private static final GolemKeyFrame[] KF_Die_RArm1 = new GolemKeyFrame[5];
	private static final GolemKeyFrame[] KF_Die_RArm2 = new GolemKeyFrame[5];
	private static final GolemKeyFrame[] KF_Die_RChest = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Die_LChest = new GolemKeyFrame[1];
	private static final GolemKeyFrame[] KF_Die_WAIST = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Die_HIP = new GolemKeyFrame[3];
	private static final GolemKeyFrame[] KF_Die_LFHip = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Die_RRHip = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Die_RFHip = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Die_LRHip = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Die_RLEG = new GolemKeyFrame[5];
	private static final GolemKeyFrame[] KF_Die_RLeg1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Die_RLeg2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Die_LLEG = new GolemKeyFrame[5];
	private static final GolemKeyFrame[] KF_Die_LLeg1 = new GolemKeyFrame[5];
	private static final GolemKeyFrame[] KF_Die_LLeg2 = new GolemKeyFrame[5];

	private static void buildDie()
	{
		KF_Die_HEAD[0] = new GolemKeyFrame(0, 0, -54, 0, 0, 0, 0);

		KF_Die_LRShoulder2[0] = new GolemKeyFrame(0, -4.22F, -42, 18, -30, 0, -15);
		KF_Die_LRShoulder2[1] = new GolemKeyFrame(37, -4.22F, -42, 18, -30, 0, -15);
		KF_Die_LRShoulder2[2] = new GolemKeyFrame(47, -29.22F, 50, 18, -30, 0, 100);
		KF_Die_LRShoulder2[3] = new GolemKeyFrame(54, -29.22F, 50, 18, -30, 0, 100);

		KF_Die_LFShoulder1[0] = new GolemKeyFrame(0, 5.52F, -47, 5.5F, -15, 0, 15);

		KF_Die_RRShoulder2[0] = new GolemKeyFrame(0, -4.2F, -42, -18, 30, 0, -15);
		KF_Die_RRShoulder2[1] = new GolemKeyFrame(35, -4.2F, -42, -18, 30, 0, -15);
		KF_Die_RRShoulder2[2] = new GolemKeyFrame(45, -21.2F, 47, -18, 30, 0, 100);
		KF_Die_RRShoulder2[3] = new GolemKeyFrame(54, -21.2F, 47, -18, 30, 0, 100);

		KF_Die_RFShoulder1[0] = new GolemKeyFrame(0, 5.5F, -47, -5.5F, 15, 0, 15);
		KF_Die_LRShoulder1[0] = new GolemKeyFrame(0, -5.5F, -47, 5.5F, -15, 0, -15);
		KF_Die_RRShoulder1[0] = new GolemKeyFrame(0, -5.52F, -47, -5.5F, 15, 0, -15);

		KF_Die_LFShoulder2[0] = new GolemKeyFrame(0, 4.21F, -42, 18, -30, 0, 15);
		KF_Die_LFShoulder2[1] = new GolemKeyFrame(34, 4.21F, -42, 18, -30, 0, 15);
		KF_Die_LFShoulder2[2] = new GolemKeyFrame(44, 4.21F, 55, 18, -30, 0, 200);
		KF_Die_LFShoulder2[3] = new GolemKeyFrame(54, 4.21F, 55, 18, -30, 0, 200);

		KF_Die_RFShoulder2[0] = new GolemKeyFrame(0, 4.2F, -42, -18, 30, 0, 15);
		KF_Die_RFShoulder2[1] = new GolemKeyFrame(38, 4.2F, -42, -18, 30, 0, 15);
		KF_Die_RFShoulder2[2] = new GolemKeyFrame(48, -8.8F, 50, -18, 30, 0, 100);
		KF_Die_RFShoulder2[3] = new GolemKeyFrame(54, -8.8F, 50, -18, 30, 0, 100);

		KF_Die_LARM[0] = new GolemKeyFrame(0, 0, -40, 21, 40, 0, 0);
		KF_Die_LARM[1] = new GolemKeyFrame(5, 0, -40, 21, 40, 0, 30);
		KF_Die_LARM[2] = new GolemKeyFrame(32, 0, -40, 21, 40, 0, 30);
		KF_Die_LARM[3] = new GolemKeyFrame(42, -10, 43, 19, 40, 0, 0);
		KF_Die_LARM[4] = new GolemKeyFrame(49, -10, 43, 19, 40, 0, 0);
		KF_Die_LARM[5] = new GolemKeyFrame(54, -45, -25, 19, 40, 0, 0);

		KF_Die_LArm1[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, -25, 0, 0);
		KF_Die_LArm1[1] = new GolemKeyFrame(26, 0.02F, 12, 0.01F, -25, 0, 0);
		KF_Die_LArm1[2] = new GolemKeyFrame(36, 27.02F, 54, -56.99F, 0, 5, -22);
		KF_Die_LArm1[3] = new GolemKeyFrame(49, 27.02F, 54, -56.99F, 0, 5, -22);
		KF_Die_LArm1[4] = new GolemKeyFrame(54, 27.02F, 0, -56.99F, 0, 5, -22);

		KF_Die_LArm2[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, -10, 0, 0);
		KF_Die_LArm2[1] = new GolemKeyFrame(20, 0.02F, 12, 0.01F, -10, 0, 0);
		KF_Die_LArm2[2] = new GolemKeyFrame(30, 48.02F, 79, 0.01F, -10, 0, 100);
		KF_Die_LArm2[3] = new GolemKeyFrame(40, 48.02F, 79, 0.01F, -10, 0, 100);
		KF_Die_LArm2[4] = new GolemKeyFrame(45, 10.02F, 0, 0.01F, -10, 0, 100);
		KF_Die_LArm2[5] = new GolemKeyFrame(54, 10.02F, 0, 0.01F, -10, 0, 100);

		KF_Die_RARM[0] = new GolemKeyFrame(0, 0, -40, -21, -40, 0, 0);
		KF_Die_RARM[1] = new GolemKeyFrame(5, 0, -40, -21, -40, 0, -109);
		KF_Die_RARM[2] = new GolemKeyFrame(30, 0, -40, -21, -40, 0, -109);
		KF_Die_RARM[3] = new GolemKeyFrame(40, -15, 54, -21, -40, 0, -109);
		KF_Die_RARM[4] = new GolemKeyFrame(49, -15, 54, -21, -40, 0, -109);
		KF_Die_RARM[5] = new GolemKeyFrame(54, -32, 4, 5, -40, 0, -109);

		KF_Die_RArm1[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, 25, 0, 0);
		KF_Die_RArm1[1] = new GolemKeyFrame(27, 0.02F, 12, 0.01F, 25, 0, 0);
		KF_Die_RArm1[2] = new GolemKeyFrame(37, -86.98F, -32, -22, 0, 0, 0);
		KF_Die_RArm1[3] = new GolemKeyFrame(49, -86.98F, -32, -22, 0, 0, 0);
		KF_Die_RArm1[4] = new GolemKeyFrame(54, 0, 0, 0, 0, 0, 0);

		KF_Die_RArm2[0] = new GolemKeyFrame(0, 0.02F, 12, 0.01F, 10, 0, 0);
		KF_Die_RArm2[1] = new GolemKeyFrame(19, 0.02F, 12, 0.01F, 10, 0, 0);
		KF_Die_RArm2[2] = new GolemKeyFrame(29, -93.98F, -36, 0.01F, 10, 0, 100);
		KF_Die_RArm2[3] = new GolemKeyFrame(49, -93.98F, -36, 0.01F, 10, 0, 100);
		KF_Die_RArm2[4] = new GolemKeyFrame(54, 0, 0, 0, 10, 0, 100);

		KF_Die_RChest[0] = new GolemKeyFrame(0, 0, -38, -8, 0, 0, 0);
		KF_Die_LChest[0] = new GolemKeyFrame(0, 0, -38, 8, 0, 0, 0);

		KF_Die_WAIST[0] = new GolemKeyFrame(0, 0, -25, 0, 0, 0, 0);
		KF_Die_WAIST[1] = new GolemKeyFrame(5, 0, -22, 7, -10, 0, 0);
		KF_Die_WAIST[2] = new GolemKeyFrame(49, 0, -22, 7, -10, 0, 0);
		KF_Die_WAIST[3] = new GolemKeyFrame(54, 0, 25, 7, -67, 0, 0);

		KF_Die_HIP[0] = new GolemKeyFrame(0, 0, -9, 0, 0, 90, 0);
		KF_Die_HIP[1] = new GolemKeyFrame(5, 0, -6, 7, 0, 90, 0);
		KF_Die_HIP[2] = new GolemKeyFrame(54, 0, -6, 7, 0, 90, 0);

		KF_Die_LFHip[0] = new GolemKeyFrame(0, 5.5F, -9, -5.5F, 15, 0, 15);
		KF_Die_LFHip[1] = new GolemKeyFrame(33, 5.5F, -9, -5.5F, 15, 0, 15);
		KF_Die_LFHip[2] = new GolemKeyFrame(43, 5.5F, 51, -5.5F, 15, 0, 100);
		KF_Die_LFHip[3] = new GolemKeyFrame(54, 5.5F, 51, -5.5F, 15, 0, 100);

		KF_Die_RRHip[0] = new GolemKeyFrame(0, -5.5F, -9, 5.5F, -15, 0, -15);
		KF_Die_RRHip[1] = new GolemKeyFrame(36, -5.5F, -9, 5.5F, -15, 0, -15);
		KF_Die_RRHip[2] = new GolemKeyFrame(46, -5.5F, 46, 5.5F, -15, 0, 200);
		KF_Die_RRHip[3] = new GolemKeyFrame(54, -5.5F, 46, 5.5F, -15, 0, 200);

		KF_Die_RFHip[0] = new GolemKeyFrame(0, 5.52F, -9, 5.5F, -15, 0, 15);
		KF_Die_RFHip[1] = new GolemKeyFrame(28, 5.52F, -9, 5.5F, -15, 0, 15);
		KF_Die_RFHip[2] = new GolemKeyFrame(38, 5.52F, 47, 5.5F, -15, 0, 100);
		KF_Die_RFHip[3] = new GolemKeyFrame(54, 5.52F, 47, 5.5F, -15, 0, 100);

		KF_Die_LRHip[0] = new GolemKeyFrame(0, -5.52F, -9, 5.5F, 15, 0, -15);
		KF_Die_LRHip[1] = new GolemKeyFrame(39, -5.52F, -9, 5.5F, 15, 0, -15);
		KF_Die_LRHip[2] = new GolemKeyFrame(49, -5.52F, 50, -5.5F, 15, 0, 100);
		KF_Die_LRHip[3] = new GolemKeyFrame(54, -5.52F, 50, -5.5F, 15, 0, 100);

		KF_Die_RLEG[0] = new GolemKeyFrame(0, 0, -11, -7, -20, 0, 0);
		KF_Die_RLEG[1] = new GolemKeyFrame(5, 0, -10, -7, -20, 0, 35);
		KF_Die_RLEG[2] = new GolemKeyFrame(26, 0, -10, -7, -20, 0, 35);
		KF_Die_RLEG[3] = new GolemKeyFrame(36, 0, 38, -7, 0, 0, 0);
		KF_Die_RLEG[4] = new GolemKeyFrame(54, 0, 38, -7, 0, 0, 0);

		KF_Die_RLeg1[0] = new GolemKeyFrame(0, 0.02F, 13.3F, -0.5F, 20, 0, 0);
		KF_Die_RLeg1[1] = new GolemKeyFrame(22, 0.02F, 13.3F, -0.5F, 20, 0, 0);
		KF_Die_RLeg1[2] = new GolemKeyFrame(32, 22.02F, 54.3F, 11.5F, 20, 0, 0);
		KF_Die_RLeg1[3] = new GolemKeyFrame(54, 22.02F, 54.3F, 11.5F, 20, 0, 0);

		KF_Die_RLeg2[0] = new GolemKeyFrame(0, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Die_RLeg2[1] = new GolemKeyFrame(17, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Die_RLeg2[2] = new GolemKeyFrame(27, 27.01F, 48, 0.01F, 0, 0, 100);
		KF_Die_RLeg2[3] = new GolemKeyFrame(54, 27.01F, 48, 0.01F, 0, 0, 100);

		KF_Die_LLEG[0] = new GolemKeyFrame(0, 0, -11, 7, 20, 0, 0);
		KF_Die_LLEG[1] = new GolemKeyFrame(5, 0, -11, 7, 20, 0, -20);
		KF_Die_LLEG[2] = new GolemKeyFrame(24, 0, -11, 7, 20, 0, -20);
		KF_Die_LLEG[3] = new GolemKeyFrame(34, 0, 40, 7, 0, 0, 0);
		KF_Die_LLEG[4] = new GolemKeyFrame(54, 0, 40, 7, 0, 0, 0);

		KF_Die_LLeg1[0] = new GolemKeyFrame(0, 0.02F, 13.3F, 0.5F, -20, 0, 0);
		KF_Die_LLeg1[1] = new GolemKeyFrame(5, 0.02F, 13.3F, 0.5F, -20, 0, 10);
		KF_Die_LLeg1[2] = new GolemKeyFrame(21, 0.02F, 13.3F, 0.5F, -20, 0, 10);
		KF_Die_LLeg1[3] = new GolemKeyFrame(31, -10.98F, 48.3F, -11.5F, -20, 0, 10);
		KF_Die_LLeg1[4] = new GolemKeyFrame(54, -10.98F, 48.3F, -11.5F, -20, 0, 10);

		KF_Die_LLeg2[0] = new GolemKeyFrame(0, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Die_LLeg2[1] = new GolemKeyFrame(5, 0.01F, 14, 0.01F, 0, 0, 10);
		KF_Die_LLeg2[2] = new GolemKeyFrame(15, 0.01F, 14, 0.01F, 0, 0, 10);
		KF_Die_LLeg2[3] = new GolemKeyFrame(25, 0.01F, 50, 0.01F, 0, 0, 100);
		KF_Die_LLeg2[4] = new GolemKeyFrame(54, 0.01F, 50, 0.01F, 0, 0, 100);
	}

	private void die(int frame, float partialTick)
	{
		GolemKeyFrame.apply(frame, HIP, KF_Die_HIP, partialTick);
		GolemKeyFrame.apply(frame, WAIST, KF_Die_WAIST, partialTick);
		GolemKeyFrame.apply(frame, LFHip, KF_Die_LFHip, partialTick);
		GolemKeyFrame.apply(frame, RChest, KF_Die_RChest, partialTick);
		GolemKeyFrame.apply(frame, LRShoulder2, KF_Die_LRShoulder2, partialTick);
		GolemKeyFrame.apply(frame, RArm1, KF_Die_RArm1, partialTick);
		GolemKeyFrame.apply(frame, LFShoulder1, KF_Die_LFShoulder1, partialTick);
		GolemKeyFrame.apply(frame, LArm2, KF_Die_LArm2, partialTick);
		GolemKeyFrame.apply(frame, LARM, KF_Die_LARM, partialTick);
		GolemKeyFrame.apply(frame, RLeg2, KF_Die_RLeg2, partialTick);
		GolemKeyFrame.apply(frame, RRShoulder2, KF_Die_RRShoulder2, partialTick);
		GolemKeyFrame.apply(frame, RFShoulder1, KF_Die_RFShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RRHip, KF_Die_RRHip, partialTick);
		GolemKeyFrame.apply(frame, LLEG, KF_Die_LLEG, partialTick);
		GolemKeyFrame.apply(frame, LRShoulder1, KF_Die_LRShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RLeg1, KF_Die_RLeg1, partialTick);
		GolemKeyFrame.apply(frame, LArm1, KF_Die_LArm1, partialTick);
		GolemKeyFrame.apply(frame, LFShoulder2, KF_Die_LFShoulder2, partialTick);
		GolemKeyFrame.apply(frame, LLeg2, KF_Die_LLeg2, partialTick);
		GolemKeyFrame.apply(frame, RArm2, KF_Die_RArm2, partialTick);
		GolemKeyFrame.apply(frame, LChest, KF_Die_LChest, partialTick);
		GolemKeyFrame.apply(frame, RFShoulder2, KF_Die_RFShoulder2, partialTick);
		GolemKeyFrame.apply(frame, LLeg1, KF_Die_LLeg1, partialTick);
		GolemKeyFrame.apply(frame, RFHip, KF_Die_RFHip, partialTick);
		GolemKeyFrame.apply(frame, HEAD, KF_Die_HEAD, partialTick);
		GolemKeyFrame.apply(frame, RRShoulder1, KF_Die_RRShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RARM, KF_Die_RARM, partialTick);
		GolemKeyFrame.apply(frame, RLEG, KF_Die_RLEG, partialTick);
		GolemKeyFrame.apply(frame, LRHip, KF_Die_LRHip, partialTick);
	}

	// ================================================================================================
	// Build
	// ================================================================================================

	private static final GolemKeyFrame[] KF_Build_HEAD = new GolemKeyFrame[3];
	private static final GolemKeyFrame[] KF_Build_LRShoulder2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_LFShoulder1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_RRShoulder2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_RFShoulder1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_LRShoulder1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_LFShoulder2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_RFShoulder2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_RRShoulder1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_LARM = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_LArm1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_LArm2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_RARM = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_RArm1 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_RArm2 = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_RChest = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_LChest = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_WAIST = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_LFHip = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_RRHip = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_RFHip = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_LRHip = new GolemKeyFrame[4];
	private static final GolemKeyFrame[] KF_Build_RLEG = new GolemKeyFrame[3];
	private static final GolemKeyFrame[] KF_Build_RLeg1 = new GolemKeyFrame[3];
	private static final GolemKeyFrame[] KF_Build_RLeg2 = new GolemKeyFrame[3];
	private static final GolemKeyFrame[] KF_Build_LLEG = new GolemKeyFrame[3];
	private static final GolemKeyFrame[] KF_Build_LLeg1 = new GolemKeyFrame[3];
	private static final GolemKeyFrame[] KF_Build_LLeg2 = new GolemKeyFrame[3];
	private static final GolemKeyFrame[] KF_Build_HIP = new GolemKeyFrame[1];

	private static void buildBuild()
	{
		KF_Build_HIP[0] = new GolemKeyFrame(0, 0, -9, 0, 0, 90, 0);

		KF_Build_LFHip[0] = new GolemKeyFrame(0, 26.5F, 49, -51.5F, 140, -170, 112);
		KF_Build_LFHip[1] = new GolemKeyFrame(10, 26.5F, 49, -51.5F, 140, -170, 112);
		KF_Build_LFHip[2] = new GolemKeyFrame(30, 5.5F, -9, -5.5F, 15, 0, 15);
		KF_Build_LFHip[3] = new GolemKeyFrame(90, 5.5F, -9, -5.5F, 15, 0, 15);

		KF_Build_RChest[0] = new GolemKeyFrame(0, 0, 60, -49, -222, 222, 111);
		KF_Build_RChest[1] = new GolemKeyFrame(20, 0, 60, -49, -222, 222, 111);
		KF_Build_RChest[2] = new GolemKeyFrame(40, 0, -38, -8, 0, 0, 0);
		KF_Build_RChest[3] = new GolemKeyFrame(90, 0, -38, -8, 0, 0, 0);

		KF_Build_LRShoulder2[0] = new GolemKeyFrame(0, -55.22F, 60, 23, -200, 90, 50);
		KF_Build_LRShoulder2[1] = new GolemKeyFrame(30, -55.22F, 60, 23, -200, 90, 50);
		KF_Build_LRShoulder2[2] = new GolemKeyFrame(50, -4.22F, -42, 18, -30, 0, -15);
		KF_Build_LRShoulder2[3] = new GolemKeyFrame(90, -4.22F, -42, 18, -30, 0, -15);

		KF_Build_RArm1[0] = new GolemKeyFrame(0, -63.98F, 93, 46.01F, 81, -91, -55);
		KF_Build_RArm1[1] = new GolemKeyFrame(45, -63.98F, 93, 46.01F, 81, -91, -55);
		KF_Build_RArm1[2] = new GolemKeyFrame(65, 0.02F, 12, 0.01F, 25, 0, 0);
		KF_Build_RArm1[3] = new GolemKeyFrame(90, 0.02F, 12, 0.01F, 25, 0, 0);

		KF_Build_LFShoulder1[0] = new GolemKeyFrame(0, 32.52F, 50, 29.5F, 230, 170, 100);
		KF_Build_LFShoulder1[1] = new GolemKeyFrame(25, 32.52F, 50, 29.5F, 230, 170, 100);
		KF_Build_LFShoulder1[2] = new GolemKeyFrame(45, 5.52F, -47, 5.5F, -15, 0, 15);
		KF_Build_LFShoulder1[3] = new GolemKeyFrame(90, 5.52F, -47, 5.5F, -15, 0, 15);

		KF_Build_LArm2[0] = new GolemKeyFrame(0, 49.02F, 87, 40.01F, 30, 88, 120);
		KF_Build_LArm2[1] = new GolemKeyFrame(50, 49.02F, 87, 40.01F, 30, 88, 120);
		KF_Build_LArm2[2] = new GolemKeyFrame(70, 0.02F, 12, 0.01F, -10, 0, 0);
		KF_Build_LArm2[3] = new GolemKeyFrame(90, 0.02F, 12, 0.01F, -10, 0, 0);

		KF_Build_LARM[0] = new GolemKeyFrame(0, -52, 64, 55, 88, 220, 19);
		KF_Build_LARM[1] = new GolemKeyFrame(35, -52, 64, 55, 88, 220, 19);
		KF_Build_LARM[2] = new GolemKeyFrame(55, 0, -40, 21, 40, 0, 0);
		KF_Build_LARM[3] = new GolemKeyFrame(90, 0, -40, 21, 40, 0, 0);

		KF_Build_RLeg2[0] = new GolemKeyFrame(0, -34.99F, 56, -10.99F, 56, -50, 200);
		KF_Build_RLeg2[1] = new GolemKeyFrame(20, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Build_RLeg2[2] = new GolemKeyFrame(90, 0.01F, 16, 0.01F, 0, 0, 0);

		KF_Build_RRShoulder2[0] = new GolemKeyFrame(0, -47.2F, 50, -33, -50, 200, 90);
		KF_Build_RRShoulder2[1] = new GolemKeyFrame(30, -47.2F, 50, -33, -50, 200, 90);
		KF_Build_RRShoulder2[2] = new GolemKeyFrame(50, -4.2F, -42, -18, 30, 0, -15);
		KF_Build_RRShoulder2[3] = new GolemKeyFrame(90, -4.2F, -42, -18, 30, 0, -15);

		KF_Build_RFShoulder1[0] = new GolemKeyFrame(0, 59.5F, 50, -58.5F, 100, 200, -200);
		KF_Build_RFShoulder1[1] = new GolemKeyFrame(25, 59.5F, 50, -58.5F, 100, 200, -200);
		KF_Build_RFShoulder1[2] = new GolemKeyFrame(45, 5.5F, -47, -5.5F, 15, 0, 15);
		KF_Build_RFShoulder1[3] = new GolemKeyFrame(90, 5.5F, -47, -5.5F, 15, 0, 15);

		KF_Build_RRHip[0] = new GolemKeyFrame(0, -41.5F, 53, 37.5F, 222, 111, 200);
		KF_Build_RRHip[1] = new GolemKeyFrame(10, -41.5F, 53, 37.5F, 222, 111, 200);
		KF_Build_RRHip[2] = new GolemKeyFrame(30, -5.5F, -9, 5.5F, -15, 0, -15);
		KF_Build_RRHip[3] = new GolemKeyFrame(90, -5.5F, -9, 5.5F, -15, 0, -15);

		KF_Build_LLEG[0] = new GolemKeyFrame(0, -20, 42, 20, 200, 200, 200);
		KF_Build_LLEG[1] = new GolemKeyFrame(20, 0, -11, 7, 20, 0, 0);
		KF_Build_LLEG[2] = new GolemKeyFrame(90, 0, -11, 7, 20, 0, 0);

		KF_Build_LRShoulder1[0] = new GolemKeyFrame(0, -15.5F, 55, 35.5F, 120, -200, 160);
		KF_Build_LRShoulder1[1] = new GolemKeyFrame(25, -15.5F, 55, 35.5F, 120, -200, 160);
		KF_Build_LRShoulder1[2] = new GolemKeyFrame(45, -5.5F, -47, 5.5F, -15, 0, -15);
		KF_Build_LRShoulder1[3] = new GolemKeyFrame(90, -5.5F, -47, 5.5F, -15, 0, -15);

		KF_Build_RLeg1[0] = new GolemKeyFrame(0, 0.02F, 41.3F, -13.5F, 20, -60, 0);
		KF_Build_RLeg1[1] = new GolemKeyFrame(20, 0.02F, 13.3F, -0.5F, 20, 0, 0);
		KF_Build_RLeg1[2] = new GolemKeyFrame(90, 0.02F, 13.3F, -0.5F, 20, 0, 0);

		KF_Build_LArm1[0] = new GolemKeyFrame(0, -48.98F, 74, -59.99F, 80, 120, 200);
		KF_Build_LArm1[1] = new GolemKeyFrame(46, -48.98F, 74, -59.99F, 80, 120, 200);
		KF_Build_LArm1[2] = new GolemKeyFrame(66, 0.02F, 12, 0.01F, -25, 0, 0);
		KF_Build_LArm1[3] = new GolemKeyFrame(90, 0.02F, 12, 0.01F, -25, 0, 0);

		KF_Build_LFShoulder2[0] = new GolemKeyFrame(0, 45.21F, 56, 44, 160, 200, -200);
		KF_Build_LFShoulder2[1] = new GolemKeyFrame(30, 45.21F, 56, 44, 160, 200, -200);
		KF_Build_LFShoulder2[2] = new GolemKeyFrame(50, 4.21F, -42, 18, -30, 0, 15);
		KF_Build_LFShoulder2[3] = new GolemKeyFrame(90, 4.21F, -42, 18, -30, 0, 15);

		KF_Build_LLeg2[0] = new GolemKeyFrame(0, 22.01F, 48, -20.99F, 100, -100, 50);
		KF_Build_LLeg2[1] = new GolemKeyFrame(20, 0.01F, 16, 0.01F, 0, 0, 0);
		KF_Build_LLeg2[2] = new GolemKeyFrame(90, 0.01F, 16, 0.01F, 0, 0, 0);

		KF_Build_RArm2[0] = new GolemKeyFrame(0, 13.02F, 90, 0.01F, 200, 190, 55);
		KF_Build_RArm2[1] = new GolemKeyFrame(48, 13.02F, 90, 0.01F, 200, 190, 55);
		KF_Build_RArm2[2] = new GolemKeyFrame(68, 0.02F, 12, 0.01F, 10, 0, 0);
		KF_Build_RArm2[3] = new GolemKeyFrame(90, 0.02F, 12, 0.01F, 10, 0, 0);

		KF_Build_LChest[0] = new GolemKeyFrame(0, 0, 60, 63, 220, -170, 160);
		KF_Build_LChest[1] = new GolemKeyFrame(20, 0, 60, 63, 220, -170, 160);
		KF_Build_LChest[2] = new GolemKeyFrame(40, 0, -38, 8, 0, 0, 0);
		KF_Build_LChest[3] = new GolemKeyFrame(90, 0, -38, 8, 0, 0, 0);

		KF_Build_RFShoulder2[0] = new GolemKeyFrame(0, 38.2F, 60, -35, -90, 120, -60);
		KF_Build_RFShoulder2[1] = new GolemKeyFrame(30, 38.2F, 60, -35, -90, 120, -60);
		KF_Build_RFShoulder2[2] = new GolemKeyFrame(50, 4.2F, -42, -18, 30, 0, 15);
		KF_Build_RFShoulder2[3] = new GolemKeyFrame(90, 4.2F, -42, -18, 30, 0, 15);

		KF_Build_LLeg1[0] = new GolemKeyFrame(0, 12.02F, 38.3F, 13.5F, 100, 201, 0);
		KF_Build_LLeg1[1] = new GolemKeyFrame(20, 0.02F, 13.3F, 0.5F, -20, 0, 0);
		KF_Build_LLeg1[2] = new GolemKeyFrame(90, 0.02F, 13.3F, 0.5F, -20, 0, 0);

		KF_Build_RFHip[0] = new GolemKeyFrame(0, 27.52F, 50, 42.5F, -200, 70, -80);
		KF_Build_RFHip[1] = new GolemKeyFrame(10, 27.52F, 50, 42.5F, -200, 70, -80);
		KF_Build_RFHip[2] = new GolemKeyFrame(30, 5.52F, -9, 5.5F, -15, 0, 15);
		KF_Build_RFHip[3] = new GolemKeyFrame(90, 5.52F, -9, 5.5F, -15, 0, 15);

		KF_Build_HEAD[0] = new GolemKeyFrame(0, 70, 54, 0, 33, 48, 160);
		KF_Build_HEAD[1] = new GolemKeyFrame(70, 70, 54, 0, 33, 48, 160);
		KF_Build_HEAD[2] = new GolemKeyFrame(90, 0, -54, 0, 0, 0, 0);

		KF_Build_RRShoulder1[0] = new GolemKeyFrame(0, -40, 70, -43.5F, 200, 200, 90);
		KF_Build_RRShoulder1[1] = new GolemKeyFrame(25, -40, 70, -43.5F, 200, 200, 90);
		KF_Build_RRShoulder1[2] = new GolemKeyFrame(45, -5.52F, -47, -5.5F, 15, 0, -15);
		KF_Build_RRShoulder1[3] = new GolemKeyFrame(90, -5.52F, -47, -5.5F, 15, 0, -15);

		KF_Build_RARM[0] = new GolemKeyFrame(0, -3, 51, -63, -101, 42, -28);
		KF_Build_RARM[1] = new GolemKeyFrame(40, -3, 51, -63, -101, 42, -28);
		KF_Build_RARM[2] = new GolemKeyFrame(60, 0, -40, -21, -40, 0, 0);
		KF_Build_RARM[3] = new GolemKeyFrame(90, 0, -40, -21, -40, 0, 0);

		KF_Build_RLEG[0] = new GolemKeyFrame(0, 35, 63, -26, 100, 150, 10);
		KF_Build_RLEG[1] = new GolemKeyFrame(20, 0, -11, -7, -20, 0, 0);
		KF_Build_RLEG[2] = new GolemKeyFrame(90, 0, -11, -7, -20, 0, 0);

		KF_Build_LRHip[0] = new GolemKeyFrame(0, -40.52F, 53, -50.5F, 222, 111, 111);
		KF_Build_LRHip[1] = new GolemKeyFrame(10, -40.52F, 53, -50.5F, 222, 111, 111);
		KF_Build_LRHip[2] = new GolemKeyFrame(30, -5.52F, -9, -5.5F, 15, 0, -15);
		KF_Build_LRHip[3] = new GolemKeyFrame(90, -5.52F, -9, -5.5F, 15, 0, -15);

		KF_Build_WAIST[0] = new GolemKeyFrame(0, 0, 46, 39, -25, 24, -22);
		KF_Build_WAIST[1] = new GolemKeyFrame(15, 0, 46, 39, -25, 24, -22);
		KF_Build_WAIST[2] = new GolemKeyFrame(35, 0, -25, 0, 0, 0, 0);
		KF_Build_WAIST[3] = new GolemKeyFrame(90, 0, -25, 0, 0, 0, 0);
	}

	private void build(int frame, float partialTick)
	{
		GolemKeyFrame.apply(frame, HIP, KF_Build_HIP, partialTick);
		GolemKeyFrame.apply(frame, WAIST, KF_Build_WAIST, partialTick);
		GolemKeyFrame.apply(frame, LFHip, KF_Build_LFHip, partialTick);
		GolemKeyFrame.apply(frame, RChest, KF_Build_RChest, partialTick);
		GolemKeyFrame.apply(frame, LRShoulder2, KF_Build_LRShoulder2, partialTick);
		GolemKeyFrame.apply(frame, RArm1, KF_Build_RArm1, partialTick);
		GolemKeyFrame.apply(frame, LFShoulder1, KF_Build_LFShoulder1, partialTick);
		GolemKeyFrame.apply(frame, LArm2, KF_Build_LArm2, partialTick);
		GolemKeyFrame.apply(frame, LARM, KF_Build_LARM, partialTick);
		GolemKeyFrame.apply(frame, RLeg2, KF_Build_RLeg2, partialTick);
		GolemKeyFrame.apply(frame, RRShoulder2, KF_Build_RRShoulder2, partialTick);
		GolemKeyFrame.apply(frame, RFShoulder1, KF_Build_RFShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RRHip, KF_Build_RRHip, partialTick);
		GolemKeyFrame.apply(frame, LLEG, KF_Build_LLEG, partialTick);
		GolemKeyFrame.apply(frame, LRShoulder1, KF_Build_LRShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RLeg1, KF_Build_RLeg1, partialTick);
		GolemKeyFrame.apply(frame, LArm1, KF_Build_LArm1, partialTick);
		GolemKeyFrame.apply(frame, LFShoulder2, KF_Build_LFShoulder2, partialTick);
		GolemKeyFrame.apply(frame, LLeg2, KF_Build_LLeg2, partialTick);
		GolemKeyFrame.apply(frame, RArm2, KF_Build_RArm2, partialTick);
		GolemKeyFrame.apply(frame, LChest, KF_Build_LChest, partialTick);
		GolemKeyFrame.apply(frame, RFShoulder2, KF_Build_RFShoulder2, partialTick);
		GolemKeyFrame.apply(frame, LLeg1, KF_Build_LLeg1, partialTick);
		GolemKeyFrame.apply(frame, RFHip, KF_Build_RFHip, partialTick);
		GolemKeyFrame.apply(frame, HEAD, KF_Build_HEAD, partialTick);
		GolemKeyFrame.apply(frame, RRShoulder1, KF_Build_RRShoulder1, partialTick);
		GolemKeyFrame.apply(frame, RARM, KF_Build_RARM, partialTick);
		GolemKeyFrame.apply(frame, RLEG, KF_Build_RLEG, partialTick);
		GolemKeyFrame.apply(frame, LRHip, KF_Build_LRHip, partialTick);
	}

	static
	{
		buildStand();
		buildThrow();
		buildRoll();
		buildStomp();
		buildDie();
		buildBuild();
	}
}
