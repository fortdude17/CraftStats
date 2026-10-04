#!/usr/bin/env bash
set -u
mkdir -p research-out
LOOM=1.13.6
decompile() { # mc version
  local mc=$1 dir=/tmp/decomp-$1
  mkdir -p $dir && cd $dir
  echo "pluginManagement { repositories { maven { url = 'https://maven.fabricmc.net/' }; gradlePluginPortal() } }" > settings.gradle
  printf "plugins { id 'fabric-loom' version '%s' }\ndependencies { minecraft 'com.mojang:minecraft:%s'; mappings loom.officialMojangMappings() }\n" $LOOM $mc > build.gradle
  gradle --no-daemon -q genSources > /tmp/gen-$mc.log 2>&1 || { tail -30 /tmp/gen-$mc.log; return 1; }
  local src=$(find ~/.gradle $dir/.gradle -name "minecraft-merged-$mc-*sources.jar" 2>/dev/null | head -1)
  mkdir -p src && cd src && unzip -q -o "$src"
  cd "$GITHUB_WORKSPACE"
}
decompile 1.21.1 &
decompile 1.21.11 &
wait
for mc in 1.21.1 1.21.11; do
  echo "################################ MC $mc"
  bash .github/research/queries.sh /tmp/decomp-$mc/src
done

# Loader APIs via javap
mkdir -p /tmp/jars && cd /tmp/jars
get() { curl -fsSL -o "$2" "$1" || echo "download failed: $1"; }
get https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.172/neoforge-21.1.172-universal.jar nf211.jar
get https://maven.neoforged.net/releases/net/neoforged/neoforge/21.11.45/neoforge-21.11.45-universal.jar nf2111.jar
get https://maven.neoforged.net/releases/net/neoforged/fancymodloader/loader/4.0.24/loader-4.0.24.jar fml4.jar
get https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/17.0.1/modmenu-17.0.1.jar modmenu17.jar
get https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/11.0.3/modmenu-11.0.3.jar modmenu11.jar
for j in nf211 nf2111; do
  echo "######## $j"; unzip -l $j.jar | grep -E "IConfigScreenFactory|ModContainer|FMLEnvironment" | head
  javap -cp $j.jar net.neoforged.neoforge.client.gui.IConfigScreenFactory 2>&1 | head -10
done
for j in modmenu11 modmenu17; do echo "######## $j"; javap -cp $j.jar com.terraformersmc.modmenu.api.ModMenuApi com.terraformersmc.modmenu.api.ConfigScreenFactory 2>&1 | head -20; done
echo "######## neoforge 21.11 META-INF"; unzip -l nf2111.jar | grep -i "fml\|loader" | head -5
find ~/.gradle -name "*fml*loader*.jar" -o -name "loader-*.jar" 2>/dev/null | head
