package com.craftstats.common.preset;

import com.craftstats.common.stats.TargetType;
import com.google.gson.annotations.SerializedName;

public class Preset {

    @SerializedName("name")        public String     name;
    @SerializedName("target_type") public TargetType targetType;
    @SerializedName("seed")        public long       seed        = 0L;
    @SerializedName("readonly")    public boolean    readonly    = false;
    @SerializedName("stats")       public Object     stats;

    public Preset() {}

    public Preset(String name, TargetType type, Object stats) {
        this.name = name;
        this.targetType = type;
        this.stats = stats;
    }
}
