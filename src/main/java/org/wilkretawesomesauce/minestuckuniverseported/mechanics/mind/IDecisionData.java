package org.wilkretawesomesauce.minestuckuniverseported.mechanics.mind;

import javax.annotation.Nullable;
import java.util.UUID;

/** See {@link DecisionData}'s own doc comment for the whole system this backs. */
public interface IDecisionData
{
	float getCertainty();

	float getHesitation();

	float getAdaptability();

	float getResolve();

	@Nullable
	DecisionType getCurrentDecision();

	@Nullable
	UUID getCurrentDecisionTarget();
}
