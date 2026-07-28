package org.wilkretawesomesauce.minestuckuniverseported.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code client.models.ModelBubble} - a single 16x16x16 box,
 * the exact same body-cube dimensions vanilla's own Slime uses (the original literally reused that
 * shape rather than building a true sphere mesh). {@code client.render.BubbleRenderer} tints and
 * alpha-blends this same cube per-instance rather than needing a dedicated sphere mesh.
 * <p>
 * <b>Real bug fix, confirmed via a live report ("bubble is usually rendered below the target")</b>: the
 * box used to be {@code addBox(-8, -16, -8, 16, 16, 16)} (local Y span {@code [-16,0]}). For this plain
 * {@code EntityRenderer}/{@code ModelPart} pipeline (no {@code LivingEntityRenderer}/GeckoLib flip in the
 * way - {@code BubbleRenderer} extends the bare {@code EntityRenderer}), local model Y+ maps directly to
 * world Y+ with no inversion, so that span put the box's top exactly at the entity's render anchor and
 * everything else a full block <i>below</i> it. That's backwards from {@link BubbleEntity#getDimensions}'s
 * bounding box ({@code EntityDimensions.scalable}), which - like every other vanilla entity - spans
 * <i>upward</i> from the anchor (anchor = bottom/feet). Every bubble-spawning tech positions the bubble's
 * anchor at roughly the caster/target's own feet, so gameplay (repel/contain/suffocate, which all read
 * {@link BubbleEntity#getBoundingBox()}) correctly enclosed the target's upper body, while the visible
 * mesh rendered entirely below their feet instead - functionally present but invisible, with an empty
 * gap where the player could actually see it working. Fixed by spanning the box upward from the anchor
 * instead ({@code addBox(-8, 0, -8, 16, 16, 16)}), matching the bounding box direction; the size math in
 * {@code BubbleRenderer} was already correct and needed no change.
 */
public final class BubbleModel
{
	private BubbleModel()
	{
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, 0.0F, -8.0F, 16, 16, 16),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}
}
