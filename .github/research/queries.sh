#!/usr/bin/env bash
cd "$1"
ls net/minecraft/resources/
grep -n "public .*(" net/minecraft/resources/ResourceKey.java | head -20
