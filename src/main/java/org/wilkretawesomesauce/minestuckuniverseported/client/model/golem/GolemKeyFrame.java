package org.wilkretawesomesauce.minestuckuniverseported.client.model.golem;

/**
 * Ported from ModularBosses (1.8)'s {@code client.models.KeyFrame} - one hand-authored animation
 * keyframe (a Tabula-style bone pose: local pivot offset + rotation, in degrees) at a given frame
 * number. {@link GolemModel} owns dozens of these arrays, one per bone per animation state; see
 * {@link #interpolate} for how a frame number between two keyframes is resolved.
 */
public class GolemKeyFrame
{
	public final int frame;
	public final float posX;
	public final float posY;
	public final float posZ;
	public final float rotX;
	public final float rotY;
	public final float rotZ;

	public GolemKeyFrame(int frame, float posX, float posY, float posZ, float rotX, float rotY, float rotZ)
	{
		this.frame = frame;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		this.rotX = rotX;
		this.rotY = rotY;
		this.rotZ = rotZ;
	}

	/**
	 * Real bug fix, caught from a live client crash ({@code ArrayIndexOutOfBoundsException} reading
	 * {@code keys[i + 1]}): the original's own {@code ModelUtils#getKeyFrameNum} has this exact same
	 * unguarded {@code keyArray[x + 1]} access, but never actually hit it in practice, because 1.8's
	 * equivalent aniFrame-transition logic ran unconditionally on both logical sides every tick (only
	 * the gameplay side effects were server-gated), so client and server always capped {@code aniFrame}
	 * in lockstep. This port's {@link org.wilkretawesomesauce.minestuckuniverseported.entity.GolemEntity}
	 * only runs that transition logic server-side (see its own doc comment) - the client's local
	 * {@code aniFrame} keeps counting up every tick until the next synced {@code aniID} packet arrives,
	 * so it can genuinely run past the last keyframe's frame number for a tick or more. Bounding the loop
	 * to {@code keys.length - 1} and falling through to the last index instead of {@code 0} means an
	 * overrun frame just holds the animation's final pose (a reasonable "waiting on the next state" look)
	 * rather than crashing or snapping back to the first pose.
	 */
	private static int keyFrameIndex(int frame, GolemKeyFrame[] keys)
	{
		if(keys.length == 1)
			return 0;

		for(int i = 0; i < keys.length - 1; i++)
		{
			if(frame == keys[i].frame)
				return i;
			if(frame > keys[i].frame && frame < keys[i + 1].frame)
				return i;
		}
		return keys.length - 1;
	}

	/**
	 * Ported from ModularBosses (1.8)'s {@code util.ModelUtils#moveParts}/{@code #getKeyFrameNum} -
	 * linearly interpolates {@code part}'s pose between whichever two keyframes in {@code keys}
	 * bracket {@code frame}, blended one further step by {@code partialTick} for smooth
	 * sub-tick rendering. Degrees are converted to radians on the way out, matching
	 * {@link net.minecraft.client.model.geom.ModelPart}'s own {@code xRot}/{@code yRot}/{@code zRot} fields.
	 */
	public static void apply(int frame, net.minecraft.client.model.geom.ModelPart part, GolemKeyFrame[] keys, float partialTick)
	{
		int keyId = keyFrameIndex(frame, keys);
		GolemKeyFrame curKey = keys[keyId];

		if(keys.length == 1 || frame == 0 || keyId == keys.length - 1)
		{
			part.x = curKey.posX;
			part.y = curKey.posY;
			part.z = curKey.posZ;
			part.xRot = curKey.rotX * 0.0174533F;
			part.yRot = curKey.rotY * 0.0174533F;
			part.zRot = curKey.rotZ * 0.0174533F;
		}
		else
		{
			GolemKeyFrame nextKey = keys[keyId + 1];
			part.x = lerp(frame, partialTick, curKey.frame, nextKey.frame, curKey.posX, nextKey.posX);
			part.y = lerp(frame, partialTick, curKey.frame, nextKey.frame, curKey.posY, nextKey.posY);
			part.z = lerp(frame, partialTick, curKey.frame, nextKey.frame, curKey.posZ, nextKey.posZ);
			part.xRot = lerp(frame, partialTick, curKey.frame, nextKey.frame, curKey.rotX, nextKey.rotX) * 0.0174533F;
			part.yRot = lerp(frame, partialTick, curKey.frame, nextKey.frame, curKey.rotY, nextKey.rotY) * 0.0174533F;
			part.zRot = lerp(frame, partialTick, curKey.frame, nextKey.frame, curKey.rotZ, nextKey.rotZ) * 0.0174533F;
		}
	}

	private static float lerp(int frame, float partialTick, int curFrame, int nextFrame, float curValue, float nextValue)
	{
		float step = (nextValue - curValue) / (nextFrame - curFrame);
		float position = (frame - curFrame) * step;
		float nextPosition = (frame + 1 - curFrame) * step;
		return curValue + position + (partialTick * (nextPosition - position));
	}
}
