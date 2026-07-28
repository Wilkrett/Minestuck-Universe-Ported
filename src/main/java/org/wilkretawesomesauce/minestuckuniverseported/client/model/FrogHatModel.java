package org.wilkretawesomesauce.minestuckuniverseported.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code client.models.armor.ModelFrogHat} - a single flat,
 * unrotated 17-box shape (a little frog-face-shaped cap), same rendering-bug category as
 * {@link WizardHatModel}/{@link ArchmageHatModel} (that class's own doc comment has the full story): the
 * imported texture was authored for this exact box layout, not vanilla's plain single-box helmet UV
 * template, so without this real model it would sample as garbled noise on a real player the same way the
 * wizard/archmage hats did.
 * <p>
 * Geometry transcribed by the {@code LegacyModelConverter} tool. Simpler than the other two hats - no
 * nested rotated sub-parts here, just one part with a flat box list - but the same real
 * {@code bipedHead}/{@code bipedHeadwear} default-box discard applies, reproduced the same way (rebuilding
 * both from an empty {@link CubeListBuilder} at vanilla's own real default {@link PartPose}).
 */
public final class FrogHatModel
{
	private FrogHatModel()
	{
	}

	public static LayerDefinition createLayer()
	{
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition partdefinition = mesh.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		head.addOrReplaceChild("froghat",
				CubeListBuilder.create()
						.texOffs(7, 16).addBox(-4.0F, -11.0F, -5.5F, 3, 3, 2)
						.texOffs(24, 0).addBox(1.0F, -11.0F, -5.5F, 3, 3, 2)
						.texOffs(0, 0).addBox(-1.0F, -10.0F, -5.0F, 2, 2, 1)
						.texOffs(0, 16).addBox(-5.0F, -8.5F, -5.0F, 1, 8, 5)
						.texOffs(20, 9).addBox(-5.0F, -8.5F, 0.0F, 1, 6, 4)
						.texOffs(4, 4).addBox(-4.5F, -0.5F, -5.0F, 0, 1, 1)
						.texOffs(0, 15).addBox(-4.5F, -0.5F, -3.0F, 0, 1, 1)
						.texOffs(4, 3).addBox(-4.5F, -0.5F, -1.0F, 0, 1, 1)
						.texOffs(12, 16).addBox(4.0F, -8.5F, -5.0F, 1, 8, 5)
						.texOffs(24, 25).addBox(4.0F, -8.5F, 0.0F, 1, 6, 4)
						.texOffs(0, 5).addBox(4.5F, -0.5F, -5.0F, 0, 1, 1)
						.texOffs(2, 5).addBox(4.5F, -0.5F, -3.0F, 0, 1, 1)
						.texOffs(4, 5).addBox(4.5F, -0.5F, -1.0F, 0, 1, 1)
						.texOffs(0, 9).addBox(-4.5F, -8.5F, 4.0F, 9, 6, 1)
						.texOffs(0, 3).addBox(-1.5F, -2.5F, 5.0F, 3, 1, 0)
						.texOffs(0, 0).addBox(-4.0F, -9.0F, -4.0F, 8, 1, 8, new CubeDeformation(-0.01F))
						.texOffs(0, 4).addBox(-1.0F, -8.0F, -5.0F, 2, 2, 0),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		// Vanilla's own outer-layer "hat" overlay stays a real, deliberately empty part - the original
		// never attached anything to bipedHeadwear either, just discarded its default box.
		partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}
}
