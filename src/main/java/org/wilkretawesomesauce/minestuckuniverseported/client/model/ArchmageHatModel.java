package org.wilkretawesomesauce.minestuckuniverseported.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code client.models.armor.ModelArchmageHat} - the same real
 * rim + 8-tapering-layer conical shape as {@link WizardHatModel} (see that class's own doc comment for the
 * rendering bug this fixes), plus two extra parts the plain wizard hat doesn't have: a star ornament
 * ({@code wizardStar}, real negative {@code CubeDeformation} shrink) and a flat base disc ({@code bottom},
 * a real zero-height box). Attaches directly under {@code bipedHead}/{@code "head"} (no intermediate
 * {@code Head} wrapper part like the plain wizard hat has - the original genuinely doesn't have one here).
 * <p>
 * Geometry transcribed by the {@code LegacyModelConverter} tool, not hand-typed - see
 * {@link WizardHatModel}'s own doc comment for what that means for the {@code "head"}/{@code "hat"}
 * vanilla-default-box replacement below.
 */
public final class ArchmageHatModel
{
	private ArchmageHatModel()
	{
	}

	public static LayerDefinition createLayer()
	{
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition partdefinition = mesh.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition wizardHat = head.addOrReplaceChild("wizardHat",
				CubeListBuilder.create(),
				PartPose.offset(0.0F, -8.0F, 0.0F));
		PartDefinition whRim = wizardHat.addOrReplaceChild("whRim",
				CubeListBuilder.create()
						.texOffs(0, 30).addBox(-3.0F, -1.0F, -7.2426F, 6, 1, 3)
						.texOffs(24, 14).addBox(-3.0F, -1.0F, 4.2426F, 6, 1, 3),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		whRim.addOrReplaceChild("cube_r1",
				CubeListBuilder.create()
						.texOffs(20, 10).addBox(-11.2071F, -1.0F, -6.1716F, 6, 1, 3),
				PartPose.offsetAndRotation(6.5607F, 0.0F, 5.0459F, 0.0F, -0.7854F, 0.0F));
		whRim.addOrReplaceChild("cube_r2",
				CubeListBuilder.create()
						.texOffs(15, 27).addBox(-9.0711F, -1.0F, -3.2145F, 6, 1, 3),
				PartPose.offsetAndRotation(4.0282F, 0.0F, 6.0711F, 0.0F, -1.5708F, 0.0F));
		whRim.addOrReplaceChild("cube_r3",
				CubeListBuilder.create()
						.texOffs(15, 23).addBox(-8.3787F, -1.0F, -1.5858F, 6, 1, 3),
				PartPose.offsetAndRotation(7.9246F, 0.0F, 0.318F, 0.0F, 0.7854F, 0.0F));
		whRim.addOrReplaceChild("cube_r4",
				CubeListBuilder.create()
						.texOffs(21, 19).addBox(-0.6213F, -1.0F, -1.5858F, 6, 1, 3),
				PartPose.offsetAndRotation(-5.8033F, 0.0F, 2.4393F, 0.0F, -0.7854F, 0.0F));
		whRim.addOrReplaceChild("cube_r5",
				CubeListBuilder.create()
						.texOffs(0, 26).addBox(0.0F, -1.0F, -1.7145F, 6, 1, 3),
				PartPose.offsetAndRotation(-5.9571F, 0.0F, -3.0F, 0.0F, -1.5708F, 0.0F));
		whRim.addOrReplaceChild("cube_r6",
				CubeListBuilder.create()
						.texOffs(0, 22).addBox(-0.6213F, -1.0F, -3.3431F, 6, 1, 3),
				PartPose.offsetAndRotation(-4.4393F, 0.0F, -1.0754F, 0.0F, 0.7854F, 0.0F));

		PartDefinition whLayer1 = wizardHat.addOrReplaceChild("whLayer1",
				CubeListBuilder.create()
						.texOffs(28, 31).addBox(-2.0F, -3.0F, -5.0F, 4, 2, 1)
						.texOffs(18, 31).addBox(-2.0F, -3.0F, 3.6569F, 4, 2, 1),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		whLayer1.addOrReplaceChild("cube_r7",
				CubeListBuilder.create()
						.texOffs(30, 23).addBox(-2.0F, -2.0F, -4.0F, 4, 2, 1),
				PartPose.offsetAndRotation(0.5858F, -1.0F, -0.7574F, 0.0F, -0.7854F, 0.0F));
		whLayer1.addOrReplaceChild("cube_r8",
				CubeListBuilder.create()
						.texOffs(0, 34).addBox(0.9069F, -2.0F, -2.0F, 4, 2, 1),
				PartPose.offsetAndRotation(2.8284F, -1.0F, -3.0784F, 0.0F, -1.5708F, 0.0F));
		whLayer1.addOrReplaceChild("cube_r9",
				CubeListBuilder.create()
						.texOffs(30, 7).addBox(-5.5282F, -2.0F, 9.4645F, 4, 2, 1),
				PartPose.offsetAndRotation(-1.4905F, -1.0F, -6.6517F, 0.0F, 0.7854F, 0.0F));
		whLayer1.addOrReplaceChild("cube_r10",
				CubeListBuilder.create()
						.texOffs(30, 27).addBox(-2.0F, -2.0F, 3.0F, 4, 2, 1),
				PartPose.offsetAndRotation(-0.5858F, -1.0F, 0.4142F, 0.0F, -0.7854F, 0.0F));
		whLayer1.addOrReplaceChild("cube_r11",
				CubeListBuilder.create()
						.texOffs(10, 34).addBox(-7.75F, -2.0F, -2.0F, 4, 2, 1),
				PartPose.offsetAndRotation(-5.8284F, -1.0F, 5.5784F, 0.0F, -1.5708F, 0.0F));
		whLayer1.addOrReplaceChild("cube_r12",
				CubeListBuilder.create()
						.texOffs(30, 4).addBox(-5.0F, -2.0F, -6.0F, 4, 2, 1),
				PartPose.offsetAndRotation(2.9497F, -1.0F, -1.4645F, 0.0F, 0.7854F, 0.0F));

		PartDefinition whLayer2 = wizardHat.addOrReplaceChild("whLayer2",
				CubeListBuilder.create()
						.texOffs(7, 40).addBox(-1.5F, -4.7462F, -4.3372F, 3, 2, 1, new CubeDeformation(0.25F))
						.texOffs(40, 6).addBox(-1.5F, -4.7462F, 2.6126F, 3, 2, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0873F, 0.0F, 0.0F));
		whLayer2.addOrReplaceChild("cube_r13",
				CubeListBuilder.create()
						.texOffs(40, 3).addBox(2.3777F, -4.7462F, -0.4225F, 3, 2, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-0.2301F, 0.0F, -5.6161F, 0.0F, -0.7854F, 0.0F));
		whLayer2.addOrReplaceChild("cube_r14",
				CubeListBuilder.create()
						.texOffs(31, 40).addBox(-6.062F, -4.7462F, -1.7751F, 3, 2, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(2.1997F, 0.0F, 4.1997F, 0.0F, -1.5708F, 0.0F));
		whLayer2.addOrReplaceChild("cube_r15",
				CubeListBuilder.create()
						.texOffs(39, 29).addBox(-8.3881F, -4.7462F, -4.3116F, 3, 2, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(10.023F, 0.0F, -0.0806F, 0.0F, 0.7854F, 0.0F));
		whLayer2.addOrReplaceChild("cube_r16",
				CubeListBuilder.create()
						.texOffs(40, 0).addBox(-3.1704F, -4.7462F, -2.978F, 3, 2, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-3.0282F, 0.0F, 5.0282F, 0.0F, -0.7854F, 0.0F));
		whLayer2.addOrReplaceChild("cube_r17",
				CubeListBuilder.create()
						.texOffs(40, 22).addBox(0.8877F, -4.7462F, -1.7751F, 3, 2, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-4.75F, 0.0F, -2.75F, 0.0F, -1.5708F, 0.0F));
		whLayer2.addOrReplaceChild("cube_r18",
				CubeListBuilder.create()
						.texOffs(39, 25).addBox(-4.9384F, -4.7462F, -4.3116F, 3, 2, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(2.6694F, 0.0F, -2.5555F, 0.0F, 0.7854F, 0.0F));

		PartDefinition whLayer3 = wizardHat.addOrReplaceChild("whLayer3",
				CubeListBuilder.create()
						.texOffs(30, 0).addBox(-1.5F, -6.2348F, -4.1736F, 3, 2, 2)
						.texOffs(24, 38).addBox(-1.5F, -6.2348F, 2.069F, 3, 2, 1),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1745F, 0.0F, 0.0F));
		whLayer3.addOrReplaceChild("cube_r19",
				CubeListBuilder.create()
						.texOffs(0, 4).addBox(-4.6228F, -6.2348F, -1.6228F, 3, 2, 2),
				PartPose.offsetAndRotation(3.6213F, 0.0F, 0.2426F, 0.0F, -0.7854F, 0.0F));
		whLayer3.addOrReplaceChild("cube_r20",
				CubeListBuilder.create()
						.texOffs(39, 13).addBox(0.3264F, -6.2348F, -1.5F, 3, 2, 1),
				PartPose.offsetAndRotation(2.1213F, 0.0F, -2.3787F, 0.0F, -1.5708F, 0.0F));
		whLayer3.addOrReplaceChild("cube_r21",
				CubeListBuilder.create()
						.texOffs(8, 37).addBox(-5.3772F, -6.2348F, -1.6228F, 3, 2, 1),
				PartPose.offsetAndRotation(5.7426F, 0.0F, -0.2929F, 0.0F, 0.7854F, 0.0F));
		whLayer3.addOrReplaceChild("cube_r22",
				CubeListBuilder.create()
						.texOffs(35, 10).addBox(0.3772F, -6.2348F, -1.6228F, 3, 2, 1),
				PartPose.offsetAndRotation(-4.3284F, 0.0F, 1.1213F, 0.0F, -0.7854F, 0.0F));
		whLayer3.addOrReplaceChild("cube_r23",
				CubeListBuilder.create()
						.texOffs(16, 38).addBox(-5.6736F, -6.2348F, -1.5F, 3, 2, 1),
				PartPose.offsetAndRotation(-4.1213F, 0.0F, 3.6213F, 0.0F, -1.5708F, 0.0F));
		whLayer3.addOrReplaceChild("cube_r24",
				CubeListBuilder.create()
						.texOffs(0, 0).addBox(0.6228F, -6.2348F, -1.6228F, 3, 2, 2),
				PartPose.offsetAndRotation(-2.9142F, 0.0F, -0.4645F, 0.0F, 0.7854F, 0.0F));

		PartDefinition whLayer4 = wizardHat.addOrReplaceChild("whLayer4",
				CubeListBuilder.create()
						.texOffs(0, 37).addBox(-1.0F, -6.9397F, -3.592F, 2, 2, 2, new CubeDeformation(0.25F))
						.texOffs(0, 8).addBox(-1.0F, -6.9397F, 0.9435F, 2, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3491F, 0.0F, 0.0F));
		whLayer4.addOrReplaceChild("cube_r25",
				CubeListBuilder.create()
						.texOffs(34, 36).addBox(-1.2418F, -6.9397F, -3.4918F, 2, 2, 2, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-0.341F, 0.0F, -0.1412F, 0.0F, -0.7854F, 0.0F));
		whLayer4.addOrReplaceChild("cube_r26",
				CubeListBuilder.create()
						.texOffs(42, 32).addBox(1.658F, -6.9397F, -1.25F, 2, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(1.5178F, 0.0F, -3.4822F, 0.0F, -1.5708F, 0.0F));
		whLayer4.addOrReplaceChild("cube_r27",
				CubeListBuilder.create()
						.texOffs(40, 36).addBox(-0.7582F, -6.9397F, -3.4918F, 2, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(3.5481F, 0.0F, 3.0659F, 0.0F, 0.7854F, 0.0F));
		whLayer4.addOrReplaceChild("cube_r28",
				CubeListBuilder.create()
						.texOffs(42, 9).addBox(-1.2418F, -6.9397F, -3.4918F, 2, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-3.5481F, 0.0F, 3.0659F, 0.0F, -0.7854F, 0.0F));
		whLayer4.addOrReplaceChild("cube_r29",
				CubeListBuilder.create()
						.texOffs(42, 38).addBox(-4.342F, -6.9397F, -1.25F, 2, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-3.0178F, 0.0F, 2.5178F, 0.0F, -1.5708F, 0.0F));
		whLayer4.addOrReplaceChild("cube_r30",
				CubeListBuilder.create()
						.texOffs(36, 32).addBox(-0.7582F, -6.9397F, -3.4918F, 2, 2, 2, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(0.341F, 0.0F, -0.1412F, 0.0F, 0.7854F, 0.0F));

		PartDefinition whLayer5 = wizardHat.addOrReplaceChild("whLayer5",
				CubeListBuilder.create()
						.texOffs(36, 18).addBox(-1.0F, -8.366F, -4.0F, 2, 2, 2)
						.texOffs(39, 40).addBox(-1.0F, -8.366F, -0.1716F, 2, 2, 1),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F));
		whLayer5.addOrReplaceChild("cube_r31",
				CubeListBuilder.create()
						.texOffs(28, 34).addBox(-1.3536F, -8.366F, -3.3536F, 2, 2, 2),
				PartPose.offsetAndRotation(-0.4142F, 0.0F, -0.6716F, 0.0F, -0.7854F, 0.0F));
		whLayer5.addOrReplaceChild("cube_r32",
				CubeListBuilder.create()
						.texOffs(42, 16).addBox(-1.5F, -8.366F, -1.0F, 2, 2, 1),
				PartPose.offsetAndRotation(1.4142F, 0.0F, -1.0858F, 0.0F, -1.5708F, 0.0F));
		whLayer5.addOrReplaceChild("cube_r33",
				CubeListBuilder.create()
						.texOffs(0, 41).addBox(-0.6464F, -8.366F, -3.3536F, 2, 2, 1),
				PartPose.offsetAndRotation(3.1213F, 0.0F, 2.0355F, 0.0F, 0.7854F, 0.0F));
		whLayer5.addOrReplaceChild("cube_r34",
				CubeListBuilder.create()
						.texOffs(15, 41).addBox(-1.3536F, -8.366F, -3.3536F, 2, 2, 1),
				PartPose.offsetAndRotation(-3.1213F, 0.0F, 2.0355F, 0.0F, -0.7854F, 0.0F));
		whLayer5.addOrReplaceChild("cube_r35",
				CubeListBuilder.create()
						.texOffs(21, 41).addBox(-2.5F, -8.366F, -1.0F, 2, 2, 1),
				PartPose.offsetAndRotation(-2.4142F, 0.0F, -0.0858F, 0.0F, -1.5708F, 0.0F));
		whLayer5.addOrReplaceChild("cube_r36",
				CubeListBuilder.create()
						.texOffs(20, 34).addBox(-0.6464F, -8.366F, -3.3536F, 2, 2, 2),
				PartPose.offsetAndRotation(0.4142F, 0.0F, -0.6716F, 0.0F, 0.7854F, 0.0F));

		PartDefinition whLayer6 = wizardHat.addOrReplaceChild("whLayer6",
				CubeListBuilder.create()
						.texOffs(21, 44).addBox(-0.5F, -9.116F, -3.3928F, 1, 1, 1, new CubeDeformation(0.25F))
						.texOffs(44, 19).addBox(-0.5F, -9.116F, -1.2715F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, -0.6981F, 0.0F, 0.0F));
		whLayer6.addOrReplaceChild("cube_r37",
				CubeListBuilder.create()
						.texOffs(26, 34).addBox(-0.0455F, -2.116F, -0.7045F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(0.5732F, -7.0F, -2.1161F, 0.0F, 0.7854F, 0.0F));
		whLayer6.addOrReplaceChild("cube_r38",
				CubeListBuilder.create()
						.texOffs(13, 44).addBox(-1.1428F, -2.116F, -0.25F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(1.3107F, -7.0F, -1.1893F, 0.0F, -1.5708F, 0.0F));
		whLayer6.addOrReplaceChild("cube_r39",
				CubeListBuilder.create()
						.texOffs(6, 8).addBox(-0.0455F, -2.116F, -0.7045F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(0.5732F, -7.0F, -0.6161F, 0.0F, 0.7854F, 0.0F));
		whLayer6.addOrReplaceChild("cube_r40",
				CubeListBuilder.create()
						.texOffs(43, 11).addBox(-0.9545F, -2.116F, -0.7045F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-0.5732F, -7.0F, -0.6161F, 0.0F, -0.7854F, 0.0F));
		whLayer6.addOrReplaceChild("cube_r41",
				CubeListBuilder.create()
						.texOffs(17, 44).addBox(-1.1428F, -2.116F, -0.25F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-0.8107F, -7.0F, -1.1893F, 0.0F, -1.5708F, 0.0F));
		whLayer6.addOrReplaceChild("cube_r42",
				CubeListBuilder.create()
						.texOffs(0, 44).addBox(-0.9545F, -2.116F, -0.7045F, 1, 1, 1, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-0.5732F, -7.0F, -2.1161F, 0.0F, -0.7854F, 0.0F));

		PartDefinition whLayer7 = wizardHat.addOrReplaceChild("whLayer7",
				CubeListBuilder.create()
						.texOffs(30, 43).addBox(-0.5F, -10.3928F, -2.766F, 1, 2, 1)
						.texOffs(42, 43).addBox(-0.5F, -10.3928F, -1.3518F, 1, 2, 1),
				PartPose.offsetAndRotation(0.0F, -1.0F, -2.0F, -0.8727F, 0.0F, 0.0F));
		whLayer7.addOrReplaceChild("cube_r43",
				CubeListBuilder.create()
						.texOffs(27, 41).addBox(5.0417F, -2.3928F, 1.4583F, 1, 2, 1),
				PartPose.offsetAndRotation(-4.8033F, -8.0F, 0.4749F, 0.0F, 0.7854F, 0.0F));
		whLayer7.addOrReplaceChild("cube_r44",
				CubeListBuilder.create()
						.texOffs(34, 43).addBox(-2.266F, -2.3928F, 0.0F, 1, 2, 1),
				PartPose.offsetAndRotation(1.2071F, -8.0F, 0.2071F, 0.0F, -1.5708F, 0.0F));
		whLayer7.addOrReplaceChild("cube_r45",
				CubeListBuilder.create()
						.texOffs(5, 43).addBox(-2.0417F, -2.3928F, -0.5417F, 1, 2, 1),
				PartPose.offsetAndRotation(1.5607F, -8.0F, 0.0607F, 0.0F, -0.7854F, 0.0F));
		whLayer7.addOrReplaceChild("cube_r46",
				CubeListBuilder.create()
						.texOffs(0, 10).addBox(-0.9583F, -2.3928F, -0.5417F, 1, 2, 1),
				PartPose.offsetAndRotation(-0.1464F, -8.0F, -1.3536F, 0.0F, 0.7854F, 0.0F));
		whLayer7.addOrReplaceChild("cube_r47",
				CubeListBuilder.create()
						.texOffs(38, 43).addBox(-2.266F, -2.3928F, 0.0F, 1, 2, 1),
				PartPose.offsetAndRotation(-0.2071F, -8.0F, 0.2071F, 0.0F, -1.5708F, 0.0F));
		whLayer7.addOrReplaceChild("cube_r48",
				CubeListBuilder.create()
						.texOffs(9, 43).addBox(-2.0417F, -2.3928F, -0.5417F, 1, 2, 1),
				PartPose.offsetAndRotation(0.5607F, -8.0F, -0.9393F, 0.0F, -0.7854F, 0.0F));

		PartDefinition whLayer8 = wizardHat.addOrReplaceChild("whLayer8",
				CubeListBuilder.create()
						.texOffs(21, 3).addBox(0.0F, -10.1808F, -2.6896F, 0, 1, 1, new CubeDeformation(0.35F)),
				PartPose.offsetAndRotation(0.0F, -2.0F, -2.0F, -1.0472F, 0.0F, 0.0F));
		whLayer8.addOrReplaceChild("cube_r49",
				CubeListBuilder.create()
						.texOffs(21, 0).addBox(1.018F, -0.1808F, -1.268F, 1, 1, 0, new CubeDeformation(0.35F)),
				PartPose.offsetAndRotation(-0.1803F, -10.0F, -0.2211F, 0.0F, 0.7854F, 0.0F));
		whLayer8.addOrReplaceChild("cube_r50",
				CubeListBuilder.create()
						.texOffs(20, 0).addBox(-0.866F, -0.1808F, -0.8236F, 0, 1, 1, new CubeDeformation(0.35F)),
				PartPose.offsetAndRotation(-0.3286F, -10.0F, -1.3286F, 0.0F, -1.5708F, 0.0F));
		whLayer8.addOrReplaceChild("cube_r51",
				CubeListBuilder.create()
						.texOffs(21, 5).addBox(-1.018F, -0.1808F, -1.268F, 1, 1, 0, new CubeDeformation(0.35F)),
				PartPose.offsetAndRotation(-0.5268F, -10.0F, -0.9282F, 0.0F, -0.7854F, 0.0F));

		wizardHat.addOrReplaceChild("wizardStar",
				CubeListBuilder.create()
						.texOffs(0, 10).addBox(-4.0F, -5.75F, -1.775F, 8, 8, 4, new CubeDeformation(-2.0F)),
				PartPose.offsetAndRotation(0.0F, -2.35F, -5.5F, -0.3491F, 0.0F, 0.0F));
		wizardHat.addOrReplaceChild("bottom",
				CubeListBuilder.create()
						.texOffs(0, 46).addBox(-5.0F, -0.75F, -5.0F, 10, 0, 10),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		// Vanilla's own outer-layer "hat" overlay stays a real, deliberately empty part - see
		// WizardHatModel's own doc comment for why.
		partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}
}
