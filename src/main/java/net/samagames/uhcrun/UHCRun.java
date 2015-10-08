package net.samagames.uhcrun;

import com.google.gson.Gson;
import net.samagames.uhcrun.compatibility.GameAdaptator;
import net.samagames.uhcrun.compatibility.GameProperties;
import net.samagames.uhcrun.generator.FortressPopulator;
import net.samagames.uhcrun.generator.OrePopulator;
import net.samagames.uhcrun.generator.WorldLoader;
import net.samagames.uhcrun.hook.NMSPatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * This file is a part of the SamaGames Project CodeBase
 * This code is absolutely confidential.
 * Created by Thog
 * (C) Copyright Elydra Network 2014 & 2015
 * All rights reserved.
 */
public class UHCRun extends JavaPlugin implements Listener
{
    private Location spawnLocation;
    private Logger logger;
    private BukkitTask startTimer;
    private OrePopulator populator;

    private boolean worldLoaded;
    private PluginManager pluginManager;
    private WorldLoader worldLoader;
    private GameAdaptator adaptator;
    private GameProperties properties;


    public static UHCRun getInstance()
    {
        return (UHCRun) Bukkit.getServer().getPluginManager().getPlugin("UHCRun");
    }

    @Override
    public void onEnable()
    {
        pluginManager = getServer().getPluginManager();
        logger = this.getLogger();

        // World Loader
        pluginManager.registerEvents(this, this);


        // Copy schematics
        this.saveResource("lobby.schematic", true);
        this.saveResource("nether_1.schematic", true);
        this.saveResource("nether_2.schematic", true);


        if (pluginManager.isPluginEnabled("SamaGamesAPI"))
        {
            this.adaptator = new GameAdaptator(this);
        }

        File worldDir = new File(getDataFolder().getAbsoluteFile().getParentFile().getParentFile(), "world");
        logger.info("Checking wether world exists at : " + worldDir.getAbsolutePath());
        if (!worldDir.exists())
        {
            logger.warning("No world exists. Will be generated.");
        } else
        {
            logger.info("World found... Checking for arena file...");
            if (!this.adaptator.checkAndDownloadWorld(worldDir))
            {
                logger.severe("Error during map downloading. The world will be generated!");
            }
        }

        properties = new GameProperties();

        File gameJson = new File(this.getDataFolder().getParentFile().getParentFile(), "game.json");

        if (gameJson.exists())
        {
            try
            {
                properties = new Gson().fromJson(new InputStreamReader(new FileInputStream(gameJson), Charset.forName("UTF-8")), GameProperties.class);
            } catch (FileNotFoundException e)
            {
                logger.severe("game.json does not exist! THIS SHOULD BE IMPOSSIBLE!");
                Bukkit.shutdown();
            }
        } else
        {
            logger.severe("game.json does not exist! THIS SHOULD BE IMPOSSIBLE!");
        }

        if (this.adaptator != null)
        {
            this.adaptator.onEnable();
        }

        this.startTimer = getServer().getScheduler().runTaskTimer(this, this::postInit, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldInit(final WorldInitEvent event)
    {
        World world = event.getWorld();
        if (world.getEnvironment() == World.Environment.NORMAL)
        {
            this.setupNormalWorld(world);
        }
    }

    @Override
    public void onDisable()
    {
        if (adaptator != null)
        {
            adaptator.removeSpawn();
        }
    }

    private void postInit()
    {
        World world = getServer().getWorld("world");
        this.startTimer.cancel();

        this.worldLoaded = true;

        if (this.adaptator != null)
        {
            this.adaptator.postInit(world);
        }

        worldLoader = new WorldLoader(this);
        worldLoader.begin(world);

    }

    private void setupNormalWorld(World world)
    {
        try
        {
            NMSPatcher patcher = new NMSPatcher(properties);
            patcher.patchBiomes();
            patcher.patchPotions();
        } catch (ReflectiveOperationException e)
        {
            e.printStackTrace();
        }

        // Init custom ore populator
        populator = new OrePopulator();

        if (properties.getOptions().containsKey("ores"))
        {
            List<Map> ores = (List<Map>) properties.getOptions().get("ores");
            for (Map ore : ores)
            {
                populator.addRule(new OrePopulator.Rule((String) ore.get("id"), ((Double) ore.get("round")).intValue(), ((Double) ore.get("minY")).intValue(), ((Double) ore.get("maxY")).intValue(), ((Double) ore.get("size")).intValue()));
            }
        }

        final WorldBorder border = world.getWorldBorder();

        // Overworld settings
        border.setCenter(0D, 0D);
        border.setSize(1000);
        border.setWarningDistance(20);
        border.setWarningTime(0);
        border.setDamageBuffer(3D);
        border.setDamageAmount(2D);
        world.setGameRuleValue("naturalRegeneration", "false");
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("randomTickSpeed", "3");
        world.setFullTime(6000);

        // Register Ore Populator
        world.getPopulators().add(populator);

        // Register Fortress Populator
        world.getPopulators().add(new FortressPopulator(this, (List<Map<String, Object>>) this.properties.getOptions().getOrDefault("netherChestLoots", new ArrayList<>())));
    }

    /*    private void patchBlocks() throws ReflectiveOperationException {
            this.overrideBlock(17, "log", "LOG", new BlockOldLog().c("log"));
            this.overrideBlock(162, "log2", "LOG2", new BlockNewLog().c("log"));
        }
    */
    public boolean isWorldLoaded()
    {
        return worldLoaded;
    }


    public Location getSpawnLocation()
    {
        return spawnLocation;
    }

    public void finishGeneration(World world, long time)
    {
        logger.info("Ready in " + time + "ms");
        if (adaptator != null)
        {
            worldLoader.computeTop(world);
            adaptator.loadEnd();
        } else
        {
            logger.info("Adaptator is null, stopping server...");
            Bukkit.shutdown();
        }
    }

    public GameAdaptator getAdaptator()
    {
        return adaptator;
    }

    public void setSpawnLocation(Location spawnLocation)
    {
        this.spawnLocation = spawnLocation;
    }

    public GameProperties getProperties()
    {
        return properties;
    }
}
