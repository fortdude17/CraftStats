package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

public class BlockStats {

    @SerializedName("physics_mode")      public String  physicsMode      = "static";
    @SerializedName("hardness")          public float   hardness         = -1f;
    @SerializedName("blast_resistance")  public float   blastResistance  = -1f;
    @SerializedName("slipperiness")      public float   slipperiness     = -1f;
    @SerializedName("bounce_factor")     public float   bounceFactor     = 0.0f;

    @SerializedName("light_emission")    public int     lightEmission    = -1;
    @SerializedName("opacity")           public int     opacity          = 15;
    @SerializedName("flammability")      public int     flammability     = 0;
    @SerializedName("tick_rate")         public int     tickRate         = 20;
    @SerializedName("ticks")             public boolean ticks            = false;
    @SerializedName("spreads")           public boolean spreads          = false;
    @SerializedName("growable")          public boolean growable         = false;
    @SerializedName("replaceable")       public boolean replaceable      = false;
    @SerializedName("climbable")         public boolean climbable        = false;
    @SerializedName("waterloggable")     public boolean waterloggable    = false;

    @SerializedName("render_layer")      public String  renderLayer      = "solid";
    @SerializedName("scale")             public float   scale            = 1.0f;
    @SerializedName("ambient_occlusion") public boolean ambientOcclusion = true;
    @SerializedName("luminance")         public int     luminance        = 0;
    @SerializedName("map_color_id")      public int     mapColorId       = 0;

    @SerializedName("no_collision")            public boolean noCollision          = false;
    @SerializedName("can_fall")                public boolean canFall              = false;
    @SerializedName("drop_xp")                public int     dropXp               = -1;
    @SerializedName("push_reaction")           public String  pushReaction         = "normal";
    @SerializedName("requires_correct_tool")   public boolean requiresCorrectTool  = false;

    @SerializedName("step_damage")       public float   stepDamage       = 0.0f;
    @SerializedName("speed_modifier")    public float   speedModifier    = 1.0f;
    @SerializedName("levitate")          public boolean levitate         = false;
    @SerializedName("glow_on_step")      public boolean glowOnStep       = false;
    @SerializedName("freeze_on_step")    public boolean freezeOnStep     = false;

    @SerializedName("on_step_potion")    public String  onStepPotion     = "";
    @SerializedName("on_step_potion_level") public int  onStepPotionLevel   = 1;
    @SerializedName("on_step_potion_duration") public int onStepPotionDuration = 60;

    public BlockStats copy() {
        BlockStats c = new BlockStats();
        c.physicsMode = physicsMode; c.hardness = hardness; c.blastResistance = blastResistance;
        c.slipperiness = slipperiness; c.bounceFactor = bounceFactor; c.lightEmission = lightEmission;
        c.opacity = opacity; c.flammability = flammability; c.tickRate = tickRate;
        c.ticks = ticks; c.spreads = spreads; c.growable = growable; c.replaceable = replaceable;
        c.climbable = climbable; c.waterloggable = waterloggable; c.renderLayer = renderLayer;
        c.scale = scale; c.ambientOcclusion = ambientOcclusion; c.luminance = luminance;
        c.mapColorId = mapColorId;
        c.noCollision = noCollision; c.canFall = canFall; c.dropXp = dropXp;
        c.pushReaction = pushReaction; c.requiresCorrectTool = requiresCorrectTool;
        c.stepDamage = stepDamage; c.speedModifier = speedModifier; c.levitate = levitate;
        c.glowOnStep = glowOnStep; c.freezeOnStep = freezeOnStep;
        c.onStepPotion = onStepPotion; c.onStepPotionLevel = onStepPotionLevel;
        c.onStepPotionDuration = onStepPotionDuration;
        return c;
    }
}
