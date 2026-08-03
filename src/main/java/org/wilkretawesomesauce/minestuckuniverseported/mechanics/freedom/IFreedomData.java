package org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom;

/** See {@link FreedomData}'s own doc comment for the whole system this backs. */
public interface IFreedomData
{
	float getFreedom();

	void setFreedom(float value);

	void addFreedom(float delta);

	FreedomLevel getLevel();
}
