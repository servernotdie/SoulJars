package io.github.thebusybiscuit.souljars;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

import net.guizhanss.guizhanlibplugin.updater.GuizhanUpdater;
import org.bstats.bukkit.Metrics;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.BrokenSpawner;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.updater.GitHubBuildsUpdater;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;

import net.guizhanss.guizhanlib.minecraft.helper.entity.EntityTypeHelper;

public class SoulJars extends JavaPlugin implements Listener, SlimefunAddon {

    private static final String JAR_TEXTURE = "bd1c777ee166c47cae698ae6b769da4e2b67f468855330ad7bddd751c5293f";
    private final Map<EntityType, Integer> mobs = new EnumMap<>(EntityType.class);

    private Config cfg;
    private ItemGroup itemGroup;
    private RecipeType recipeType;
    private SlimefunItemStack emptyJar;

    @Override
    public void onEnable() {
        cfg = new Config(this);

        if (!getServer().getPluginManager().isPluginEnabled("GuizhanLibPlugin")) {
            getLogger().log(Level.SEVERE, "Plugin nay can GuizhanLibPlugin de hoat dong!");
            getLogger().log(Level.SEVERE, "Tai tai: https://50l.cc/gzlib");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Setting up bStats
        new Metrics(this, 5581);

        if (cfg.getBoolean("options.auto-update") && getDescription().getVersion().startsWith("Build")) {
            GuizhanUpdater.start(this, getFile(), "SlimefunGuguProject", "SoulJars", "master");
        }

        emptyJar = new SlimefunItemStack("SOUL_JAR", JAR_TEXTURE, "&bBinh Linh Hon &7(Trong)", "", "&rKhi o trong tui do:", "&rGiet quai de phong an linh hon");
        itemGroup = new ItemGroup(new NamespacedKey(this, "soul_jars"), new CustomItemStack(emptyJar, "&bBinh Linh Hon", "", "&a> Bam de mo"));
        recipeType = new RecipeType(new NamespacedKey(this, "mob_killing"), new CustomItemStack(Material.DIAMOND_SWORD, "&cKhi co Binh Trong trong tui do:", "&cGiet quai tuong ung"));

        new SlimefunItem(itemGroup, emptyJar, RecipeType.ANCIENT_ALTAR, new ItemStack[] { SlimefunItems.EARTH_RUNE, new ItemStack(Material.SOUL_SAND), SlimefunItems.WATER_RUNE, new ItemStack(Material.SOUL_SAND), SlimefunItems.NECROTIC_SKULL, new ItemStack(Material.SOUL_SAND), SlimefunItems.AIR_RUNE, new ItemStack(Material.SOUL_SAND), SlimefunItems.FIRE_RUNE }, new CustomItemStack(emptyJar, 3)).register(this);
        new JarsListener(this);

        for (String mob : cfg.getStringList("mobs")) {
            try {
                EntityType type = EntityType.valueOf(mob);
                registerSoul(type);
            } catch (Exception x) {
                getLogger().log(Level.WARNING, "{0}: Loai quai khong hop le: {1}", new Object[] { x.getClass().getSimpleName(), mob });
            }
        }

        cfg.save();
    }

    private void registerSoul(EntityType type) {
        String name = EntityTypeHelper.getName(type);

        int souls = cfg.getOrSetDefault("souls-required." + type, 128);
        mobs.put(type, souls);

        Material mobEgg = Material.getMaterial(type.name() + "_SPAWN_EGG");

        if (mobEgg == null) {
            mobEgg = Material.ZOMBIE_SPAWN_EGG;
        }

        // @formatter:off
        SlimefunItemStack jarItem = new SlimefunItemStack(type.name() + "_SOUL_JAR", JAR_TEXTURE, "&cBinh Linh Hon &7(" + name + ")", "", "&7Linh hon: &e1");
        SlimefunItem jar = new UnplaceableBlock(itemGroup, jarItem, recipeType,
        new ItemStack[] { null, null, null, emptyJar, null, new CustomItemStack(mobEgg, "&rGiet " + souls + " " + name + " de day"), null, null, null });
        jar.register(this);

        SlimefunItemStack filledJarItem = new SlimefunItemStack("FILLED_" + type.name() + "_SOUL_JAR", JAR_TEXTURE, "&cBinh Linh Hon Day &7(" + name + ")", "", "&7Linh hon: &e" + souls);
        SlimefunItem filledJar = new FilledJar(itemGroup, filledJarItem, recipeType,
        new ItemStack[] { null, null, null, emptyJar, null, new CustomItemStack(mobEgg, "&rGiet " + souls + " " + name), null, null, null });
        filledJar.register(this);

        BrokenSpawner brokenSpawner = SlimefunItems.BROKEN_SPAWNER.getItem(BrokenSpawner.class);

        SlimefunItemStack spawnerItem = new SlimefunItemStack(type.name() + "_BROKEN_SPAWNER", brokenSpawner.getItemForEntityType(type));
        new SlimefunItem(itemGroup, spawnerItem, RecipeType.ANCIENT_ALTAR,
        new ItemStack[] { new ItemStack(Material.IRON_BARS), SlimefunItems.EARTH_RUNE, new ItemStack(Material.IRON_BARS), SlimefunItems.EARTH_RUNE, filledJarItem, SlimefunItems.EARTH_RUNE, new ItemStack(Material.IRON_BARS), SlimefunItems.EARTH_RUNE, new ItemStack(Material.IRON_BARS) },
        brokenSpawner.getItemForEntityType(type)).register(this);
        // @formatter:on
    }

    public Map<EntityType, Integer> getRequiredSouls() {
        return mobs;
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/SlimefunGuguProject/SoulJars/issues";
    }

}
