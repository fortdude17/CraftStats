package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the {@code libs} extension.
 */
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final ArchLibraryAccessors laccForArchLibraryAccessors = new ArchLibraryAccessors(owner);
    private final ClothLibraryAccessors laccForClothLibraryAccessors = new ClothLibraryAccessors(owner);
    private final FabricLibraryAccessors laccForFabricLibraryAccessors = new FabricLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

    /**
     * Dependency provider for <b>minecraft</b> with <b>com.mojang:minecraft</b> coordinates and
     * with version reference <b>minecraft</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getMinecraft() {
        return create("minecraft");
    }

    /**
     * Dependency provider for <b>modmenu</b> with <b>com.terraformersmc:modmenu</b> coordinates and
     * with version reference <b>modmenu</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getModmenu() {
        return create("modmenu");
    }

    /**
     * Dependency provider for <b>neoforge</b> with <b>net.neoforged:neoforge</b> coordinates and
     * with version reference <b>neoforge</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getNeoforge() {
        return create("neoforge");
    }

    /**
     * Group of libraries at <b>arch</b>
     */
    public ArchLibraryAccessors getArch() {
        return laccForArchLibraryAccessors;
    }

    /**
     * Group of libraries at <b>cloth</b>
     */
    public ClothLibraryAccessors getCloth() {
        return laccForClothLibraryAccessors;
    }

    /**
     * Group of libraries at <b>fabric</b>
     */
    public FabricLibraryAccessors getFabric() {
        return laccForFabricLibraryAccessors;
    }

    /**
     * Group of versions at <b>versions</b>
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Group of bundles at <b>bundles</b>
     */
    public BundleAccessors getBundles() {
        return baccForBundleAccessors;
    }

    /**
     * Group of plugins at <b>plugins</b>
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class ArchLibraryAccessors extends SubDependencyFactory {

        public ArchLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>common</b> with <b>dev.architectury:architectury</b> coordinates and
         * with version reference <b>architectury</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCommon() {
            return create("arch.common");
        }

        /**
         * Dependency provider for <b>fabric</b> with <b>dev.architectury:architectury-fabric</b> coordinates and
         * with version reference <b>architectury</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getFabric() {
            return create("arch.fabric");
        }

        /**
         * Dependency provider for <b>neoforge</b> with <b>dev.architectury:architectury-neoforge</b> coordinates and
         * with version reference <b>architectury</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getNeoforge() {
            return create("arch.neoforge");
        }

    }

    public static class ClothLibraryAccessors extends SubDependencyFactory {
        private final ClothConfigLibraryAccessors laccForClothConfigLibraryAccessors = new ClothConfigLibraryAccessors(owner);

        public ClothLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>cloth.config</b>
         */
        public ClothConfigLibraryAccessors getConfig() {
            return laccForClothConfigLibraryAccessors;
        }

    }

    public static class ClothConfigLibraryAccessors extends SubDependencyFactory {

        public ClothConfigLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>fabric</b> with <b>me.shedaniel.cloth:cloth-config-fabric</b> coordinates and
         * with version reference <b>cloth.config</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getFabric() {
            return create("cloth.config.fabric");
        }

        /**
         * Dependency provider for <b>neoforge</b> with <b>me.shedaniel.cloth:cloth-config-neoforge</b> coordinates and
         * with version reference <b>cloth.config</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getNeoforge() {
            return create("cloth.config.neoforge");
        }

    }

    public static class FabricLibraryAccessors extends SubDependencyFactory {

        public FabricLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>net.fabricmc.fabric-api:fabric-api</b> coordinates and
         * with version reference <b>fabric.api</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getApi() {
            return create("fabric.api");
        }

        /**
         * Dependency provider for <b>loader</b> with <b>net.fabricmc:fabric-loader</b> coordinates and
         * with version reference <b>fabric.loader</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLoader() {
            return create("fabric.loader");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        private final ArchVersionAccessors vaccForArchVersionAccessors = new ArchVersionAccessors(providers, config);
        private final ClothVersionAccessors vaccForClothVersionAccessors = new ClothVersionAccessors(providers, config);
        private final FabricVersionAccessors vaccForFabricVersionAccessors = new FabricVersionAccessors(providers, config);
        private final ParchmentVersionAccessors vaccForParchmentVersionAccessors = new ParchmentVersionAccessors(providers, config);
        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>architectury</b> with value <b>13.0.8</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getArchitectury() { return getVersion("architectury"); }

        /**
         * Version alias <b>java</b> with value <b>21</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJava() { return getVersion("java"); }

        /**
         * Version alias <b>minecraft</b> with value <b>1.21.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getMinecraft() { return getVersion("minecraft"); }

        /**
         * Version alias <b>modmenu</b> with value <b>11.0.3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getModmenu() { return getVersion("modmenu"); }

        /**
         * Version alias <b>neoforge</b> with value <b>21.1.172</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getNeoforge() { return getVersion("neoforge"); }

        /**
         * Version alias <b>shadow</b> with value <b>8.3.3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getShadow() { return getVersion("shadow"); }

        /**
         * Group of versions at <b>versions.arch</b>
         */
        public ArchVersionAccessors getArch() {
            return vaccForArchVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.cloth</b>
         */
        public ClothVersionAccessors getCloth() {
            return vaccForClothVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.fabric</b>
         */
        public FabricVersionAccessors getFabric() {
            return vaccForFabricVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.parchment</b>
         */
        public ParchmentVersionAccessors getParchment() {
            return vaccForParchmentVersionAccessors;
        }

    }

    public static class ArchVersionAccessors extends VersionFactory  {

        public ArchVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>arch.loom</b> with value <b>1.7-SNAPSHOT</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLoom() { return getVersion("arch.loom"); }

        /**
         * Version alias <b>arch.plugin</b> with value <b>3.4.161</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getPlugin() { return getVersion("arch.plugin"); }

    }

    public static class ClothVersionAccessors extends VersionFactory  {

        public ClothVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>cloth.config</b> with value <b>15.0.140</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getConfig() { return getVersion("cloth.config"); }

    }

    public static class FabricVersionAccessors extends VersionFactory  {

        public FabricVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>fabric.api</b> with value <b>0.116.12+1.21.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getApi() { return getVersion("fabric.api"); }

        /**
         * Version alias <b>fabric.loader</b> with value <b>0.16.9</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLoader() { return getVersion("fabric.loader"); }

    }

    public static class ParchmentVersionAccessors extends VersionFactory  implements VersionNotationSupplier {

        public ParchmentVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>parchment</b> with value <b>2024.07.28</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> asProvider() { return getVersion("parchment"); }

        /**
         * Version alias <b>parchment.mc</b> with value <b>1.21.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getMc() { return getVersion("parchment.mc"); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

    }

    public static class PluginAccessors extends PluginFactory {
        private final ArchPluginAccessors paccForArchPluginAccessors = new ArchPluginAccessors(providers, config);

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>shadow</b> with plugin id <b>com.gradleup.shadow</b> and
         * with version reference <b>shadow</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getShadow() { return createPlugin("shadow"); }

        /**
         * Group of plugins at <b>plugins.arch</b>
         */
        public ArchPluginAccessors getArch() {
            return paccForArchPluginAccessors;
        }

    }

    public static class ArchPluginAccessors extends PluginFactory {

        public ArchPluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>arch.loom</b> with plugin id <b>dev.architectury.loom</b> and
         * with version reference <b>arch.loom</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getLoom() { return createPlugin("arch.loom"); }

        /**
         * Plugin provider for <b>arch.plugin</b> with plugin id <b>architectury-plugin</b> and
         * with version reference <b>arch.plugin</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getPlugin() { return createPlugin("arch.plugin"); }

    }

}
