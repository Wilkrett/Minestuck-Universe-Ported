package org.wilkretawesomesauce.minestuckuniverseported.damage;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import javax.annotation.Nullable;

public class CritDamageSource extends DamageSource implements IGodTierDamage
{
    boolean isCrit = false;
    boolean godproof = false;

    public CritDamageSource(Holder<DamageType> type)
    {
        super(type);
    }

    @Override
    public CritDamageSource setCrit()
    {
        isCrit = true;
        return this;
    }

    @Override
    public boolean isCrit() {
        return isCrit;
    }

    @Override
    public CritDamageSource setGodproof()
    {
        godproof = true;
        return this;
    }

    @Override
    public boolean isGodproof() {
        return godproof;
    }

    @Nullable
    public Entity getImmediateSource() {
        return null;
    }
}