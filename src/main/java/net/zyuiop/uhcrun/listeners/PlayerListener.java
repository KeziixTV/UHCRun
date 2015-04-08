package net.zyuiop.uhcrun.listeners;

import net.samagames.gameapi.GameAPI;
import net.samagames.gameapi.GameUtils;
import net.samagames.gameapi.json.Status;
import net.zyuiop.uhcrun.UHCRun;
import net.zyuiop.uhcrun.game.BasicGame;
import net.zyuiop.uhcrun.game.SoloGame;
import net.zyuiop.uhcrun.game.Team;
import net.zyuiop.uhcrun.game.TeamGame;
import net.zyuiop.uhcrun.utils.Gui;
import net.zyuiop.uhcrun.utils.GuiSelectTeam;
import net.zyuiop.uhcrun.utils.Metadatas;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * This file is a part of the SamaGames Project CodeBase
 * This code is absolutely confidential.
 * Created by {USER}
 * (C) Copyright Elydra Network 2014 & 2015
 * All rights reserved.
 */
public class PlayerListener implements Listener {

    protected BasicGame game;

    public PlayerListener(BasicGame game) {
        this.game = game;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player)
            if (!game.isDamages())
                event.setCancelled(true);
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.GOLDEN_APPLE) {
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10*20, 1));
        }
    }

    @EventHandler
    public void onConsumeBucket(PlayerBucketEmptyEvent event) {
        if (event.getBucket() == Material.LAVA_BUCKET && !game.isPvpenabled()) {
            event.getPlayer().sendMessage(ChatColor.RED + "Le placement de lave est interdit avant l'activation du PVP.");
        }
    }

    @EventHandler
    public void inventoryClose(InventoryCloseEvent event) {
        if(UHCRun.instance.getPlayerGui(event.getPlayer().getUniqueId()) != null) {
            UHCRun.instance.removePlayerFromList(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getItem() != null && event.getItem().getType() == Material.LAVA_BUCKET && !game.isPvpenabled()) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(ChatColor.RED + "Le placement de lave est interdit avant l'activation du PVP.");
				return;
			}
		}

        if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if(event.getItem() != null) {
                if (! UHCRun.instance.game.isGameStarted()) {
                    event.setCancelled(true);

                    if(event.getItem().getType() == Material.WOOD_DOOR)
                        GameAPI.kickPlayer(event.getPlayer());

                    else if (event.getItem().getType() == Material.NETHER_STAR)
                        UHCRun.instance.openGui(event.getPlayer(), new GuiSelectTeam());
                } else {
                    if(! UHCRun.instance.game.hasPlayer(event.getPlayer().getUniqueId())) {
                        event.setCancelled(true);

                        if(event.getItem().getType() == Material.WOOD_DOOR)
                            GameAPI.kickPlayer(event.getPlayer());
                    }
                }
            }
        }
    }

    @EventHandler
    public void clickEvent(InventoryClickEvent event) {
        if(event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();

            if (! UHCRun.instance.game.isGameStarted() && event.getView().getType() == InventoryType.PLAYER) {
                event.setCancelled(true);
            }

            if (event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null) {
                Gui gui = UHCRun.instance.getPlayerGui(player.getUniqueId());
                if (gui != null) {
                    String action = gui.getAction(event.getSlot());
                    if (action != null)
                        gui.onClick(player, event.getCurrentItem(), action, event.getClick());
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        BasicGame bgame = UHCRun.instance.game;
        if (!(bgame instanceof TeamGame))
            return;

        TeamGame game = (TeamGame) bgame;
        if (!game.isGameStarted()) {
            event.getBlock().setType(Material.AIR);
            Team team = game.getPlayerTeam(event.getPlayer().getUniqueId());
            String name = event.getLine(0);
            name = name.trim();

            if(!name.equals("")) {
                team.setTeamName(name);
                event.getPlayer().sendMessage(game.getCoherenceMachine().getGameTag()+ChatColor.GREEN + "Le nom de votre équipe est désormais : "+team.getChatColor()+team.getTeamName());
                UHCRun.instance.openGui(event.getPlayer(), new GuiSelectTeam());
            } else {
                event.getPlayer().sendMessage(game.getCoherenceMachine().getGameTag()+ChatColor.RED + "Le nom de l'équipe ne peut être vide.");
                UHCRun.instance.openGui(event.getPlayer(), new GuiSelectTeam());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (UHCRun.instance.game instanceof SoloGame)
            return;

        TeamGame game = (TeamGame) UHCRun.instance.game;
        if (!game.isGameStarted())
            return;

        if (event.getMessage().startsWith("!")) {
            String message = event.getMessage().substring(1);
            Team team = game.getPlayerTeam(event.getPlayer().getUniqueId());
            if (team != null) {
                event.setFormat(team.getChatColor() + "[" + team.getTeamName() + "] " + event.getPlayer().getName() + " : " + ChatColor.WHITE + message);
            }
        } else {
            Team team = game.getPlayerTeam(event.getPlayer().getUniqueId());
            if (team != null) {
                event.setCancelled(true);
                String message = team.getChatColor() + "(Equipe) " + event.getPlayer().getName() + " : " + ChatColor.GOLD + ChatColor.ITALIC + event.getMessage();
                for (UUID id : team.getPlayers()) {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null)
                        player.sendMessage(message);
                }
            }
        }
    }

    @EventHandler
    public void brewevent(BrewEvent event) {
        if(event.getContents().getIngredient().getType() == Material.GLOWSTONE_DUST)
            event.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Material mat = event.getBlock().getType();
        Location loc = event.getBlock().getLocation();

        switch (mat) {
            case IRON_ORE:
                event.getBlock().setType(Material.AIR);
                dropItem(loc, new ItemStack(Material.IRON_INGOT, 2));
                event.setCancelled(true);
                break;
            case SAND:
                event.getBlock().setType(Material.AIR);
                dropItem(loc, new ItemStack(Material.GLASS, 1));
                event.setCancelled(true);
                break;
            case GRAVEL:
                event.getBlock().setType(Material.AIR);
                if (new Random().nextDouble() < 0.75)
                    dropItem(loc, new ItemStack(Material.ARROW, 3));
                else
                    dropItem(loc, new ItemStack(Material.GRAVEL, 1));
                event.setCancelled(true);
                break;
            case GOLD_ORE:
                event.setCancelled(true);
                event.getBlock().setType(Material.AIR);
                dropItem(loc, new ItemStack(Material.GOLD_INGOT, 2));
                break;
            case COAL_ORE:
                event.setCancelled(true);
                event.getBlock().setType(Material.AIR);
                dropItem(loc, new ItemStack(Material.TORCH, 3));
                break;
            case DIAMOND_ORE:
                event.setCancelled(true);
                event.getBlock().setType(Material.AIR);
                dropItem(loc, new ItemStack(Material.DIAMOND, 2));
                break;
            case LOG: case LOG_2:
                final List<Block> bList = new ArrayList<Block>();
                bList.add(event.getBlock());
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        for (int i = 0; i < bList.size(); i++) {
                            Block block = bList.get(i);
                            if (block.getType() == Material.LOG || block.getType() == Material.LOG_2) {
                                for (ItemStack item : block.getDrops()) {
                                    block.getWorld().dropItemNaturally(block.getLocation(), item);
                                }
                                block.setType(Material.AIR);
                            }
                            for (BlockFace face : BlockFace.values()) {
                                if (block.getRelative(face).getType() == Material.LOG || block.getRelative(face).getType() == Material.LOG_2) {
                                    bList.add(block.getRelative(face));
                                }
                            }
                            bList.remove(block);
                        }
                        if (bList.size() == 0)
                            cancel();
                    }
                }.runTaskTimer(UHCRun.instance, 1, 1);
                break;
        }
        event.getPlayer().giveExp(event.getExpToDrop() * 2);
    }

    private void dropItem(final Location location, final ItemStack drop) {
        Bukkit.getScheduler().runTaskLater(UHCRun.instance, new Runnable() {
            @Override
            public void run() {
                location.getWorld().dropItemNaturally(location, drop);
            }
        }, 4);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getRecipe().getResult().getType() == Material.GOLDEN_APPLE && event.getRecipe().getResult().getDurability() == 1)
            event.getInventory().setResult(new ItemStack(Material.AIR));
        else if (event.getRecipe().getResult().getType() == Material.WOOD_PICKAXE)
            event.getInventory().setResult(new ItemStack(Material.STONE_PICKAXE));
        else if (event.getRecipe().getResult().getType() == Material.WOOD_AXE)
            event.getInventory().setResult(new ItemStack(Material.STONE_AXE));
        else if (event.getRecipe().getResult().getType() == Material.WOOD_SWORD)
            event.getInventory().setResult(new ItemStack(Material.STONE_SWORD));
        else if (event.getRecipe().getResult().getType() == Material.WOOD_SPADE)
            event.getInventory().setResult(new ItemStack(Material.STONE_SPADE));
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.WITCH)
            event.setCancelled(true);
    }

	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Location to = event.getTo();
		double max = to.getWorld().getWorldBorder().getSize() / 2.0;
		max += 50;
		if (to.getZ() > max || to.getX() > max || to.getZ() < -max || to.getX() < -max) {
			event.setCancelled(true);
			event.getPlayer().teleport(new Location(event.getTo().getWorld(), 0, 128, 0));
			event.getPlayer().sendMessage(ChatColor.RED + "Ne t'éloigne pas trop, tu pourrais te perdre.");
		}
	}

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe().getResult().getType() == Material.GOLDEN_APPLE && event.getRecipe().getResult().getDurability() == 1)
            event.getInventory().setResult(new ItemStack(Material.AIR));
        else if (event.getRecipe().getResult().getType() == Material.WOOD_PICKAXE)
            event.getInventory().setResult(new ItemStack(Material.STONE_PICKAXE));
        else if (event.getRecipe().getResult().getType() == Material.WOOD_AXE)
            event.getInventory().setResult(new ItemStack(Material.STONE_AXE));
		else if (event.getRecipe().getResult().getType() == Material.WOOD_SPADE)
			event.getInventory().setResult(new ItemStack(Material.STONE_SPADE));
    }

    @EventHandler (priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            Entity damager = event.getDamager();

            if (damager instanceof Player) {
                if (!game.isPvpenabled()) {
                    event.setCancelled(true);
                    return;
                }
                Metadatas.setMetadata(damaged, "lastDamager", (Player) damager);
            } else if (damager instanceof Projectile) {
                Projectile  arrow = (Projectile) damager;
                Entity shooter = (Entity) arrow.getShooter();
                if (shooter instanceof Player) {
                    if (!game.isPvpenabled()) {
                        event.setCancelled(true);
                        return;
                    }
                    Metadatas.setMetadata(damaged, "lastDamager", (Player) shooter);
                }
            }
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        if (game.isInGame(event.getPlayer().getUniqueId())) {
            game.stumpPlayer(event.getPlayer(), true);
            if (game.getStatus() == Status.InGame) {
                Location l = event.getPlayer().getLocation();
                World w = l.getWorld();
                for (ItemStack stack : event.getPlayer().getInventory().getContents()) {
                    if (stack != null) {
                        w.dropItemNaturally(l, stack);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (game.isInGame(event.getEntity().getUniqueId())) {
            game.stumpPlayer(event.getEntity(), false);
            event.getDrops().add(new ItemStack(Material.GOLDEN_APPLE));
            if (event.getEntity().getKiller() != null)
                event.getEntity().getKiller().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20*20, 1));
            GameUtils.broadcastSound(Sound.WITHER_DEATH);
        }
        event.setDeathMessage(game.getCoherenceMachine().getGameTag()+event.getDeathMessage());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Random random = new Random();
        LivingEntity entity = event.getEntity();
        if (entity instanceof Cow) {
            List<ItemStack> newDrops = new ArrayList<>();
            for (ItemStack stack : event.getDrops()) {
                if (stack.getType() == Material.RAW_BEEF)
                    newDrops.add(new ItemStack(Material.COOKED_BEEF, stack.getAmount()*2));
                else if (stack.getType() == Material.LEATHER)
                    newDrops.add(new ItemStack(Material.LEATHER, stack.getAmount()*2));
            }
            event.getDrops().clear();
            event.getDrops().addAll(newDrops);
        } else if (entity instanceof Sheep) {
            List<ItemStack> newDrops = new ArrayList<>();
            for (ItemStack stack : event.getDrops()) {
                if (stack.getType() == Material.MUTTON)
                    newDrops.add(new ItemStack(Material.COOKED_MUTTON, stack.getAmount()*2));
            }
            if (random.nextInt(32) >= 16)
                newDrops.add(new ItemStack(Material.LEATHER, random.nextInt(5)+1));
            if (random.nextInt(32) >= 16)
                newDrops.add(new ItemStack(Material.STRING, random.nextInt(2)+1));
            event.getDrops().clear();
            event.getDrops().addAll(newDrops);
        } else if (entity instanceof Pig) {
            List<ItemStack> newDrops = new ArrayList<>();
            for (ItemStack stack : event.getDrops()) {
                if (stack.getType() == Material.PORK)
                    newDrops.add(new ItemStack(Material.GRILLED_PORK, stack.getAmount()*2));
            }
            if (random.nextInt(32) >= 16)
                newDrops.add(new ItemStack(Material.LEATHER, random.nextInt(5)+1));
            event.getDrops().clear();
            event.getDrops().addAll(newDrops);
        } else if (entity instanceof Rabbit) {
            List<ItemStack> newDrops = new ArrayList<>();
            for (ItemStack stack : event.getDrops()) {
                if (stack.getType() == Material.RABBIT)
                    newDrops.add(new ItemStack(Material.COOKED_RABBIT, stack.getAmount()*2));
            }
            event.getDrops().clear();
            event.getDrops().addAll(newDrops);
        } else if (entity instanceof Chicken) {
            List<ItemStack> newDrops = new ArrayList<>();
            for (ItemStack stack : event.getDrops()) {
                if (stack.getType() == Material.RAW_CHICKEN)
                    newDrops.add(new ItemStack(Material.COOKED_CHICKEN, stack.getAmount()*2));
                if (stack.getType() == Material.FEATHER)
                    newDrops.add(new ItemStack(Material.ARROW, stack.getAmount()*2));
            }
            event.getDrops().clear();
            event.getDrops().addAll(newDrops);
        } else if (entity instanceof Skeleton) {
            List<ItemStack> newDrops = new ArrayList<>();
            for (ItemStack stack : event.getDrops()) {
                if (stack.getType() == Material.ARROW)
                    newDrops.add(new ItemStack(Material.ARROW, stack.getAmount()*2)) ;
                if (stack.getType() == Material.BOW) {
                    stack.setDurability((short) 0);
                    newDrops.add(stack);
                }
            }
            event.getDrops().clear();
            event.getDrops().addAll(newDrops);
        }
        event.setDroppedExp(event.getDroppedExp() * 2);
    }
}