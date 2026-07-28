package org.wilkretawesomesauce.minestuckuniverseported.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code client.models.armor.ModelWizardHat} - a genuine
 * multi-box Blockbench model (a rim + 8 tapering conical "layer" rings, ~60 boxes total), not the plain
 * flat vanilla helmet shape {@code MSUItems#WIZARD_HAT} used to render with. That mismatch (this real
 * texture sampled against vanilla's simple UV layout) is what produced the garbled "TV static" look a
 * real player report caught - see {@code items.WizardHatItem}'s own doc comment for the rendering side of
 * the fix.
 * <p>
 * Geometry transcribed by the {@code LegacyModelConverter} tool (see
 * {@code C:\Users\Wilkret\Documents\DevelopBro\LegacyModelConverter}), not hand-typed - the original's own
 * {@code bipedHead}/{@code bipedHeadwear} both got their vanilla default box discarded
 * ({@code cubeList.remove(0)}) before the real hat geometry was attached only under {@code bipedHead}
 * (nothing was ever attached to {@code bipedHeadwear}/{@code "hat"} - it stays a real, deliberately empty
 * part below, exactly matching the original), which the converter reproduces by rebuilding both parts from
 * an empty {@link CubeListBuilder} at vanilla's own real default {@link PartPose} instead of decorating the
 * untouched vanilla box.
 */
public final class WizardHatModel
{
	private WizardHatModel()
	{
	}

	public static LayerDefinition createLayer()
	{
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition partdefinition = mesh.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition Head = head.addOrReplaceChild("Head",
				CubeListBuilder.create(),
				PartPose.offset(4.0F, 31.0F, -4.5F));
		PartDefinition wizardHat = Head.addOrReplaceChild("wizardHat",
				CubeListBuilder.create(),
				PartPose.offset(-4.0F, -38.8F, 4.5F));
		PartDefinition whRim = wizardHat.addOrReplaceChild("whRim",
				CubeListBuilder.create()
						.texOffs(12, 7).addBox(-2.5F, -1.0F, -6.0F, 5, 1, 2)
						.texOffs(12, 4).addBox(-2.5F, -1.0F, 4.0711F, 5, 1, 2),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		whRim.addOrReplaceChild("cube_r1",
				CubeListBuilder.create()
						.texOffs(12, 1).addBox(-11.2071F, -1.0F, -5.1716F, 5, 1, 2),
				PartPose.offsetAndRotation(6.7678F, 0.0F, 5.5815F, 0.0F, -0.7854F, 0.0F));
		whRim.addOrReplaceChild("cube_r2",
				CubeListBuilder.create()
						.texOffs(0, 12).addBox(-8.5355F, -1.0F, -1.9645F, 5, 1, 2),
				PartPose.offsetAndRotation(4.0711F, 0.0F, 6.0711F, 0.0F, -1.5708F, 0.0F));
		whRim.addOrReplaceChild("cube_r3",
				CubeListBuilder.create()
						.texOffs(0, 9).addBox(-8.3787F, -1.0F, -1.5858F, 5, 1, 2),
				PartPose.offsetAndRotation(8.1317F, 0.0F, -0.1464F, 0.0F, 0.7854F, 0.0F));
		whRim.addOrReplaceChild("cube_r4",
				CubeListBuilder.create()
						.texOffs(0, 6).addBox(0.3787F, -1.0F, -1.5858F, 5, 1, 2),
				PartPose.offsetAndRotation(-6.0104F, 0.0F, 1.9749F, 0.0F, -0.7854F, 0.0F));
		whRim.addOrReplaceChild("cube_r5",
				CubeListBuilder.create()
						.texOffs(0, 3).addBox(0.5355F, -1.0F, -1.9645F, 5, 1, 2),
				PartPose.offsetAndRotation(-6.0F, 0.0F, -3.0F, 0.0F, -1.5708F, 0.0F));
		whRim.addOrReplaceChild("cube_r6",
				CubeListBuilder.create()
						.texOffs(0, 0).addBox(0.3787F, -1.0F, -2.3431F, 5, 1, 2),
				PartPose.offsetAndRotation(-4.6464F, 0.0F, -0.5398F, 0.0F, 0.7854F, 0.0F));

		PartDefinition whLayer1 = wizardHat.addOrReplaceChild("whLayer1",
				CubeListBuilder.create()
						.texOffs(22, 16).addBox(-2.0F, -2.0F, -5.0F, 4, 2, 1)
						.texOffs(22, 10).addBox(-2.0F, -2.0F, 3.6569F, 4, 2, 1),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		whLayer1.addOrReplaceChild("cube_r7",
				CubeListBuilder.create()
						.texOffs(20, 22).addBox(-2.0F, -2.0F, -4.0F, 4, 2, 1),
				PartPose.offsetAndRotation(0.5858F, 0.0F, -0.7574F, 0.0F, -0.7854F, 0.0F));
		whLayer1.addOrReplaceChild("cube_r8",
				CubeListBuilder.create()
						.texOffs(21, 13).addBox(0.9069F, -2.0F, -2.0F, 4, 2, 1),
				PartPose.offsetAndRotation(2.8284F, 0.0F, -3.0784F, 0.0F, -1.5708F, 0.0F));
		whLayer1.addOrReplaceChild("cube_r9",
				CubeListBuilder.create()
						.texOffs(10, 21).addBox(-5.5282F, -2.0F, 9.4645F, 4, 2, 1),
				PartPose.offsetAndRotation(-1.4905F, 0.0F, -6.6517F, 0.0F, 0.7854F, 0.0F));
		whLayer1.addOrReplaceChild("cube_r10",
				CubeListBuilder.create()
						.texOffs(0, 20).addBox(-2.0F, -2.0F, 3.0F, 4, 2, 1),
				PartPose.offsetAndRotation(-0.5858F, 0.0F, 0.4142F, 0.0F, -0.7854F, 0.0F));
		whLayer1.addOrReplaceChild("cube_r11",
				CubeListBuilder.create()
						.texOffs(19, 19).addBox(-7.75F, -2.0F, -2.0F, 4, 2, 1),
				PartPose.offsetAndRotation(-5.8284F, 0.0F, 5.5784F, 0.0F, -1.5708F, 0.0F));
		whLayer1.addOrReplaceChild("cube_r12",
				CubeListBuilder.create()
						.texOffs(9, 18).addBox(-5.0F, -2.0F, -6.0F, 4, 2, 1),
				PartPose.offsetAndRotation(2.9497F, 0.0F, -1.4645F, 0.0F, 0.7854F, 0.0F));

		PartDefinition whLayer2 = wizardHat.addOrReplaceChild("whLayer2",
				CubeListBuilder.create()
						.texOffs(31, 31).addBox(-1.5F, -3.25F, -4.25F, 3, 2, 1, new CubeDeformation(0.25F))
						.texOffs(31, 14).addBox(-1.5F, -3.25F, 2.6997F, 3, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0873F, 0.0F, 0.0F));
		whLayer2.addOrReplaceChild("cube_r13",
				CubeListBuilder.create()
						.texOffs(8, 31).addBox(2.4393F, -3.25F, -0.3609F, 3, 2, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-0.2301F, 0.0F, -5.6161F, 0.0F, -0.7854F, 0.0F));
		whLayer2.addOrReplaceChild("cube_r14",
				CubeListBuilder.create()
						.texOffs(30, 22).addBox(-5.9749F, -3.25F, -1.7751F, 3, 2, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(2.1997F, 0.0F, 4.1997F, 0.0F, -1.5708F, 0.0F));
		whLayer2.addOrReplaceChild("cube_r15",
				CubeListBuilder.create()
						.texOffs(31, 12).addBox(-8.4497F, -3.25F, -4.25F, 3, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(10.023F, 0.0F, -0.0806F, 0.0F, 0.7854F, 0.0F));
		whLayer2.addOrReplaceChild("cube_r16",
				CubeListBuilder.create()
						.texOffs(30, 25).addBox(-3.1088F, -3.25F, -2.9164F, 3, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-3.0282F, 0.0F, 5.0282F, 0.0F, -0.7854F, 0.0F));
		whLayer2.addOrReplaceChild("cube_r17",
				CubeListBuilder.create()
						.texOffs(30, 0).addBox(0.9749F, -3.25F, -1.7751F, 3, 2, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-4.75F, 0.0F, -2.75F, 0.0F, -1.5708F, 0.0F));
		whLayer2.addOrReplaceChild("cube_r18",
				CubeListBuilder.create()
						.texOffs(0, 30).addBox(-5.0F, -3.25F, -4.25F, 3, 2, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(2.6694F, 0.0F, -2.5555F, 0.0F, 0.7854F, 0.0F));

		PartDefinition whLayer3 = wizardHat.addOrReplaceChild("whLayer3",
				CubeListBuilder.create()
						.texOffs(0, 15).addBox(-1.5F, -5.0F, -4.0F, 3, 2, 2)
						.texOffs(29, 19).addBox(-1.5F, -5.0F, 2.2426F, 3, 2, 1),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1745F, 0.0F, 0.0F));
		whLayer3.addOrReplaceChild("cube_r19",
				CubeListBuilder.create()
						.texOffs(12, 14).addBox(-4.5F, -5.0F, -1.5F, 3, 2, 2),
				PartPose.offsetAndRotation(3.6213F, 0.0F, 0.2426F, 0.0F, -0.7854F, 0.0F));
		whLayer3.addOrReplaceChild("cube_r20",
				CubeListBuilder.create()
						.texOffs(24, 29).addBox(0.5F, -5.0F, -1.5F, 3, 2, 1),
				PartPose.offsetAndRotation(2.1213F, 0.0F, -2.3787F, 0.0F, -1.5708F, 0.0F));
		whLayer3.addOrReplaceChild("cube_r21",
				CubeListBuilder.create()
						.texOffs(16, 29).addBox(-5.5F, -5.0F, -1.5F, 3, 2, 1),
				PartPose.offsetAndRotation(5.7426F, 0.0F, -0.2929F, 0.0F, 0.7854F, 0.0F));
		whLayer3.addOrReplaceChild("cube_r22",
				CubeListBuilder.create()
						.texOffs(8, 28).addBox(0.5F, -5.0F, -1.5F, 3, 2, 1),
				PartPose.offsetAndRotation(-4.3284F, 0.0F, 1.1213F, 0.0F, -0.7854F, 0.0F));
		whLayer3.addOrReplaceChild("cube_r23",
				CubeListBuilder.create()
						.texOffs(0, 27).addBox(-5.5F, -5.0F, -1.5F, 3, 2, 1),
				PartPose.offsetAndRotation(-4.1213F, 0.0F, 3.6213F, 0.0F, -1.5708F, 0.0F));
		whLayer3.addOrReplaceChild("cube_r24",
				CubeListBuilder.create()
						.texOffs(12, 10).addBox(0.5F, -5.0F, -1.5F, 3, 2, 2),
				PartPose.offsetAndRotation(-2.9142F, 0.0F, -0.4645F, 0.0F, 0.7854F, 0.0F));

		PartDefinition whLayer4 = wizardHat.addOrReplaceChild("whLayer4",
				CubeListBuilder.create()
						.texOffs(26, 6).addBox(-1.0F, -6.0F, -3.25F, 2, 2, 2, new CubeDeformation(0.25F))
						.texOffs(5, 34).addBox(-1.0F, -6.0F, 1.2855F, 2, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3491F, 0.0F, 0.0F));
		whLayer4.addOrReplaceChild("cube_r25",
				CubeListBuilder.create()
						.texOffs(24, 25).addBox(-1.0F, -6.0F, -3.25F, 2, 2, 2, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-0.341F, 0.0F, -0.1412F, 0.0F, -0.7854F, 0.0F));
		whLayer4.addOrReplaceChild("cube_r26",
				CubeListBuilder.create()
						.texOffs(0, 33).addBox(2.0F, -6.0F, -1.25F, 2, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(1.5178F, 0.0F, -3.4822F, 0.0F, -1.5708F, 0.0F));
		whLayer4.addOrReplaceChild("cube_r27",
				CubeListBuilder.create()
						.texOffs(32, 10).addBox(-1.0F, -6.0F, -3.25F, 2, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(3.5481F, 0.0F, 3.0659F, 0.0F, 0.7854F, 0.0F));
		whLayer4.addOrReplaceChild("cube_r28",
				CubeListBuilder.create()
						.texOffs(32, 6).addBox(-1.0F, -6.0F, -3.25F, 2, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-3.5481F, 0.0F, 3.0659F, 0.0F, -0.7854F, 0.0F));
		whLayer4.addOrReplaceChild("cube_r29",
				CubeListBuilder.create()
						.texOffs(24, 0).addBox(-4.0F, -6.0F, -1.25F, 2, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-3.0178F, 0.0F, 2.5178F, 0.0F, -1.5708F, 0.0F));
		whLayer4.addOrReplaceChild("cube_r30",
				CubeListBuilder.create()
						.texOffs(16, 25).addBox(-1.0F, -6.0F, -3.25F, 2, 2, 2, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(0.341F, 0.0F, -0.1412F, 0.0F, 0.7854F, 0.0F));

		PartDefinition whLayer5 = wizardHat.addOrReplaceChild("whLayer5",
				CubeListBuilder.create()
						.texOffs(24, 2).addBox(-1.0F, -7.5F, -3.5F, 2, 2, 2)
						.texOffs(32, 27).addBox(-1.0F, -7.5F, 0.3284F, 2, 2, 1),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F));
		whLayer5.addOrReplaceChild("cube_r31",
				CubeListBuilder.create()
						.texOffs(8, 24).addBox(-1.0F, -7.5F, -3.0F, 2, 2, 2),
				PartPose.offsetAndRotation(-0.4142F, 0.0F, -0.6716F, 0.0F, -0.7854F, 0.0F));
		whLayer5.addOrReplaceChild("cube_r32",
				CubeListBuilder.create()
						.texOffs(32, 16).addBox(-1.0F, -7.5F, -1.0F, 2, 2, 1),
				PartPose.offsetAndRotation(1.4142F, 0.0F, -1.0858F, 0.0F, -1.5708F, 0.0F));
		whLayer5.addOrReplaceChild("cube_r33",
				CubeListBuilder.create()
						.texOffs(32, 3).addBox(-1.0F, -7.5F, -3.0F, 2, 2, 1),
				PartPose.offsetAndRotation(3.1213F, 0.0F, 2.0355F, 0.0F, 0.7854F, 0.0F));
		whLayer5.addOrReplaceChild("cube_r34",
				CubeListBuilder.create()
						.texOffs(22, 32).addBox(-1.0F, -7.5F, -3.0F, 2, 2, 1),
				PartPose.offsetAndRotation(-3.1213F, 0.0F, 2.0355F, 0.0F, -0.7854F, 0.0F));
		whLayer5.addOrReplaceChild("cube_r35",
				CubeListBuilder.create()
						.texOffs(16, 32).addBox(-2.0F, -7.5F, -1.0F, 2, 2, 1),
				PartPose.offsetAndRotation(-2.4142F, 0.0F, -0.0858F, 0.0F, -1.5708F, 0.0F));
		whLayer5.addOrReplaceChild("cube_r36",
				CubeListBuilder.create()
						.texOffs(0, 23).addBox(-1.0F, -7.5F, -3.0F, 2, 2, 2),
				PartPose.offsetAndRotation(0.4142F, 0.0F, -0.6716F, 0.0F, 0.7854F, 0.0F));

		PartDefinition whLayer6 = wizardHat.addOrReplaceChild("whLayer6",
				CubeListBuilder.create()
						.texOffs(35, 36).addBox(-0.5F, -8.35F, -2.75F, 1, 1, 1, new CubeDeformation(0.25F))
						.texOffs(26, 36).addBox(-0.5F, -8.35F, -0.6287F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, -0.6981F, 0.0F, 0.0F));
		whLayer6.addOrReplaceChild("cube_r37",
				CubeListBuilder.create()
						.texOffs(8, 36).addBox(-0.5F, -1.35F, -0.25F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(0.5732F, -7.0F, -2.1161F, 0.0F, 0.7854F, 0.0F));
		whLayer6.addOrReplaceChild("cube_r38",
				CubeListBuilder.create()
						.texOffs(4, 36).addBox(-0.5F, -1.35F, -0.25F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(1.3107F, -7.0F, -1.1893F, 0.0F, -1.5708F, 0.0F));
		whLayer6.addOrReplaceChild("cube_r39",
				CubeListBuilder.create()
						.texOffs(23, 35).addBox(-0.5F, -1.35F, -0.25F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(0.5732F, -7.0F, -0.6161F, 0.0F, 0.7854F, 0.0F));
		whLayer6.addOrReplaceChild("cube_r40",
				CubeListBuilder.create()
						.texOffs(19, 35).addBox(-0.5F, -1.35F, -0.25F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-0.5732F, -7.0F, -0.6161F, 0.0F, -0.7854F, 0.0F));
		whLayer6.addOrReplaceChild("cube_r41",
				CubeListBuilder.create()
						.texOffs(34, 8).addBox(-0.5F, -1.35F, -0.25F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-0.8107F, -7.0F, -1.1893F, 0.0F, -1.5708F, 0.0F));
		whLayer6.addOrReplaceChild("cube_r42",
				CubeListBuilder.create()
						.texOffs(22, 25).addBox(-0.5F, -1.35F, -0.25F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-0.5732F, -7.0F, -2.1161F, 0.0F, -0.7854F, 0.0F));

		PartDefinition whLayer7 = wizardHat.addOrReplaceChild("whLayer7",
				CubeListBuilder.create()
						.texOffs(15, 35).addBox(-0.5F, -9.5F, -2.0F, 1, 2, 1)
						.texOffs(14, 24).addBox(-0.5F, -9.5F, -0.5858F, 1, 1, 1),
				PartPose.offsetAndRotation(0.0F, -1.0F, -2.0F, -0.8727F, 0.0F, 0.0F));
		whLayer7.addOrReplaceChild("cube_r43",
				CubeListBuilder.create()
						.texOffs(0, 35).addBox(4.5F, -1.5F, 2.0F, 1, 2, 1),
				PartPose.offsetAndRotation(-4.8033F, -8.0F, 0.4749F, 0.0F, 0.7854F, 0.0F));
		whLayer7.addOrReplaceChild("cube_r44",
				CubeListBuilder.create()
						.texOffs(32, 34).addBox(-1.5F, -1.5F, 0.0F, 1, 2, 1),
				PartPose.offsetAndRotation(1.2071F, -8.0F, 0.2071F, 0.0F, -1.5708F, 0.0F));
		whLayer7.addOrReplaceChild("cube_r45",
				CubeListBuilder.create()
						.texOffs(6, 23).addBox(-1.5F, -1.5F, 0.0F, 1, 1, 1),
				PartPose.offsetAndRotation(1.5607F, -8.0F, 0.0607F, 0.0F, -0.7854F, 0.0F));
		whLayer7.addOrReplaceChild("cube_r46",
				CubeListBuilder.create()
						.texOffs(8, 15).addBox(-1.5F, -1.5F, 0.0F, 1, 1, 1),
				PartPose.offsetAndRotation(-0.1464F, -8.0F, -1.3536F, 0.0F, 0.7854F, 0.0F));
		whLayer7.addOrReplaceChild("cube_r47",
				CubeListBuilder.create()
						.texOffs(11, 34).addBox(-1.5F, -1.5F, 0.0F, 1, 2, 1),
				PartPose.offsetAndRotation(-0.2071F, -8.0F, 0.2071F, 0.0F, -1.5708F, 0.0F));
		whLayer7.addOrReplaceChild("cube_r48",
				CubeListBuilder.create()
						.texOffs(28, 33).addBox(-1.5F, -1.5F, 0.0F, 1, 2, 1),
				PartPose.offsetAndRotation(0.5607F, -8.0F, -0.9393F, 0.0F, -0.7854F, 0.0F));

		PartDefinition whLayer8 = wizardHat.addOrReplaceChild("whLayer8",
				CubeListBuilder.create()
						.texOffs(3, 3).addBox(0.0F, -9.1808F, -1.8236F, 0, 1, 1, new CubeDeformation(0.35F)),
				PartPose.offsetAndRotation(0.0F, -2.0F, -2.0F, -1.0472F, 0.0F, 0.0F));
		whLayer8.addOrReplaceChild("cube_r49",
				CubeListBuilder.create()
						.texOffs(3, 4).addBox(0.4056F, 0.8192F, -0.6556F, 1, 1, 0, new CubeDeformation(0.35F)),
				PartPose.offsetAndRotation(-0.1803F, -10.0F, -0.2211F, 0.0F, 0.7854F, 0.0F));
		whLayer8.addOrReplaceChild("cube_r50",
				CubeListBuilder.create()
						.texOffs(3, 3).addBox(0.0F, 0.8192F, -0.8236F, 0, 1, 1, new CubeDeformation(0.35F)),
				PartPose.offsetAndRotation(-0.3286F, -10.0F, -1.3286F, 0.0F, -1.5708F, 0.0F));
		whLayer8.addOrReplaceChild("cube_r51",
				CubeListBuilder.create()
						.texOffs(3, 4).addBox(-0.4056F, 0.8192F, -0.6556F, 1, 1, 0, new CubeDeformation(0.35F)),
				PartPose.offsetAndRotation(-0.5268F, -10.0F, -0.9282F, 0.0F, -0.7854F, 0.0F));

		// Vanilla's own outer-layer "hat" overlay stays a real, deliberately empty part - the original
		// never attached anything to bipedHeadwear either, just discarded its default box.
		partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}
}
