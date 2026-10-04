package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

/** World-wide rules. Null = vanilla. */
public class WorldStats {

    @SerializedName("v") public int schema = StatSchema.CURRENT;

    // ---- time & weather ------------------------------------------------------------------
    @SerializedName("day_length_multiplier")  public Double  dayLengthMultiplier;
    @SerializedName("always_clear")           public boolean alwaysClear;

    // ---- physics -------------------------------------------------------------------------
    @SerializedName("gravity_multiplier")     public Double  gravityMultiplier;
    @SerializedName("fall_damage_multiplier") public Double  fallDamageMultiplier;
    @SerializedName("explosion_power_multiplier") public Double explosionPowerMultiplier;

    // ---- mobs ----------------------------------------------------------------------------
    @SerializedName("mob_health_multiplier")  public Double  mobHealthMultiplier;
    @SerializedName("mob_damage_multiplier")  public Double  mobDamageMultiplier;
    @SerializedName("mob_speed_multiplier")   public Double  mobSpeedMultiplier;
    @SerializedName("spawn_cap_multiplier")   public Double  spawnCapMultiplier;
    @SerializedName("mobs_never_despawn")     public boolean mobsNeverDespawn;
    @SerializedName("mobs_ignore_players")    public boolean mobsIgnorePlayers;

    // ---- players -------------------------------------------------------------------------
    @SerializedName("player_damage_taken_multiplier") public Double playerDamageTakenMultiplier;
    @SerializedName("hunger_multiplier")      public Double  hungerMultiplier;
    @SerializedName("xp_multiplier")          public Double  xpMultiplier;
    @SerializedName("item_despawn_seconds")   public Integer itemDespawnSeconds;

    public WorldStats copy() { return StatSchema.copy(this, WorldStats.class); }
}
