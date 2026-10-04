#!/usr/bin/env bash
set -u
mkdir -p research-out
versions() { curl -fsSL "$1" | grep -o '<version>[^<]*</version>' | sed 's/<[^>]*>//g'; }
show() { echo "== $1"; versions "$2" | grep -E "${3:-.}" | sort -V | tail -${4:-40} | tr '\n' ' '; echo; }
show fabric-loom      https://maven.fabricmc.net/fabric-loom/fabric-loom.gradle.plugin/maven-metadata.xml '^1\.1[0-5]\.' 15
show fabric-loader    https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml '^0\.1[6-9]' 10
show fabric-api       https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml '1\.21\.(1|11)$' 20
show fabric-api-new   https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml '.' 8
show arch-loom        https://maven.architectury.dev/dev.architectury.loom/dev.architectury.loom.gradle.plugin/maven-metadata.xml '^1\.(7|1[0-5])' 40
show arch-plugin      https://maven.architectury.dev/architectury-plugin/architectury-plugin.gradle.plugin/maven-metadata.xml '.' 8
show architectury     https://maven.architectury.dev/dev/architectury/architectury/maven-metadata.xml '^(13|19)\.' 30
show neoforge-21.1    https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml '^21\.1\.' 5
show neoforge-21.11   https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml '^21\.11\.' 10
show neoforge-new     https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml '.' 8
show modmenu          https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/maven-metadata.xml '^1[1-7]\.' 15
echo "== mc versions"; curl -fsSL https://piston-meta.mojang.com/mc/game/version_manifest_v2.json | python3 -c "import json,sys;d=json.load(sys.stdin);print(d['latest']);print([v['id'] for v in d['versions'] if v['type']=='release'][:8])"

# Decompile Minecraft 1.21.11 with Mojang names using a throwaway Fabric Loom project.
LOOM=$(versions https://maven.fabricmc.net/fabric-loom/fabric-loom.gradle.plugin/maven-metadata.xml | grep -E '^1\.1[1-3]\.[0-9]+$' | sort -V | tail -1)
echo "Using loom $LOOM"
mkdir -p /tmp/decomp && cd /tmp/decomp
cat > settings.gradle <<S
pluginManagement { repositories { maven { url = 'https://maven.fabricmc.net/' }; gradlePluginPortal() } }
S
cat > build.gradle <<B
plugins { id 'fabric-loom' version '$LOOM' }
dependencies { minecraft 'com.mojang:minecraft:1.21.11'; mappings loom.officialMojangMappings() }
B
gradle --no-daemon -q genSources > /tmp/gen.log 2>&1 || { tail -40 /tmp/gen.log; exit 1; }
SRC=$(find ~/.gradle /tmp/decomp/.gradle -name '*minecraft-*1.21.11*-sources.jar' 2>/dev/null | head -1)
echo "Sources: $SRC"
cp "$SRC" "$GITHUB_WORKSPACE/research-out/mc-1.21.11-sources.jar"
mkdir -p src && cd src && unzip -q "$SRC"
cd "$GITHUB_WORKSPACE"
bash .github/research/queries.sh /tmp/decomp/src
