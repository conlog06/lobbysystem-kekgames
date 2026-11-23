package de.sujuc.lobby;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

final class Protection implements Listener {
    private final LobbyPlugin p;
    Protection(LobbyPlugin plugin){p=plugin;}
    boolean world(World w){return p.active()&&w.getName().equals(p.cfg.world);}
    boolean locked(Player player){return p.lobby(player)&&!p.builders.contains(player.getUniqueId());}
    boolean npc(Entity e){return e.getPersistentDataContainer().has(p.npcKey,PersistentDataType.STRING);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void place(BlockPlaceEvent e){if(locked(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){if(locked(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void damage(EntityDamageEvent e){if(world(e.getEntity().getWorld())){e.setCancelled(true);if(e.getEntity() instanceof Player player&&player.getLocation().getY()<player.getWorld().getMinHeight())p.teleport(player,"spawn");}}
    @EventHandler public void food(FoodLevelChangeEvent e){if(world(e.getEntity().getWorld()))e.setCancelled(true);}
    @EventHandler public void weather(WeatherChangeEvent e){if(world(e.getWorld())&&e.toWeatherState())e.setCancelled(true);}
    @EventHandler public void spawn(CreatureSpawnEvent e){if(world(e.getLocation().getWorld())&&e.getSpawnReason()!=CreatureSpawnEvent.SpawnReason.CUSTOM)e.setCancelled(true);}
    @EventHandler public void explosion(EntityExplodeEvent e){if(world(e.getLocation().getWorld())){e.blockList().clear();e.setCancelled(true);}}
    @EventHandler public void explosion(BlockExplodeEvent e){if(world(e.getBlock().getWorld())){e.blockList().clear();e.setCancelled(true);}}
    @EventHandler public void burn(BlockBurnEvent e){if(world(e.getBlock().getWorld()))e.setCancelled(true);}
    @EventHandler public void ignite(BlockIgniteEvent e){if(world(e.getBlock().getWorld())&&(e.getPlayer()==null||locked(e.getPlayer())))e.setCancelled(true);}
    @EventHandler public void change(EntityChangeBlockEvent e){if(world(e.getBlock().getWorld()))e.setCancelled(true);}
    @EventHandler public void drop(PlayerDropItemEvent e){if(locked(e.getPlayer()))e.setCancelled(true);}
    @EventHandler public void pickup(EntityPickupItemEvent e){if(e.getEntity() instanceof Player player&&locked(player))e.setCancelled(true);}
    @EventHandler public void swap(PlayerSwapHandItemsEvent e){if(locked(e.getPlayer()))e.setCancelled(true);}
    @EventHandler public void bucket(PlayerBucketEmptyEvent e){if(locked(e.getPlayer()))e.setCancelled(true);}
    @EventHandler public void bucket(PlayerBucketFillEvent e){if(locked(e.getPlayer()))e.setCancelled(true);}
    @EventHandler public void armor(PlayerArmorStandManipulateEvent e){if(npc(e.getRightClicked())||locked(e.getPlayer()))e.setCancelled(true);}
    @EventHandler public void hanging(org.bukkit.event.hanging.HangingBreakByEntityEvent e){if(world(e.getEntity().getWorld())&&(!(e.getRemover() instanceof Player player)||locked(player)))e.setCancelled(true);}
    @EventHandler public void projectile(ProjectileLaunchEvent e){if(e.getEntity().getShooter() instanceof Player player&&locked(player))e.setCancelled(true);}
    @EventHandler public void interact(PlayerInteractEvent e){
        if(!locked(e.getPlayer()))return;
        e.setCancelled(true);if(e.getHand()!=EquipmentSlot.HAND)return;
        if(e.getAction()==Action.RIGHT_CLICK_AIR||e.getAction()==Action.RIGHT_CLICK_BLOCK)p.menus.click(e.getPlayer(),p.menus.action(e.getItem()));
    }
    @EventHandler public void entity(PlayerInteractEntityEvent e){
        if(!world(e.getRightClicked().getWorld()))return;
        if(npc(e.getRightClicked())){
            e.setCancelled(true);if(e.getHand()==EquipmentSlot.HAND){String type=e.getRightClicked().getPersistentDataContainer().get(p.npcKey,PersistentDataType.STRING);if(type.equals("settings")||type.equals("reward"))p.menus.open(e.getPlayer(),type);}
        }else if(locked(e.getPlayer()))e.setCancelled(true);
    }
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player player))return;
        if(e.getView().getTopInventory().getHolder() instanceof Menus.Holder holder){
            e.setCancelled(true);
            if(holder.viewer.equals(player.getUniqueId())&&e.getRawSlot()>=0&&e.getRawSlot()<e.getView().getTopInventory().getSize()&&(e.getClick()==ClickType.LEFT||e.getClick()==ClickType.RIGHT)){
                String action=p.menus.action(e.getCurrentItem());Bukkit.getScheduler().runTask(p,()->{if(player.getOpenInventory().getTopInventory()==holder.inv)p.menus.click(player,action);});
            }
        }else if(locked(player))e.setCancelled(true);
    }
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Menus.Holder||e.getWhoClicked() instanceof Player player&&locked(player))e.setCancelled(true);}
    @EventHandler public void move(PlayerMoveEvent e){if(locked(e.getPlayer())&&e.getTo()!=null&&e.getTo().getY()<e.getPlayer().getWorld().getMinHeight()-2)p.teleport(e.getPlayer(),"spawn");}
    @EventHandler public void death(PlayerDeathEvent e){if(world(e.getEntity().getWorld())){e.setKeepInventory(true);e.setKeepLevel(true);e.getDrops().clear();e.setDroppedExp(0);e.setDeathMessage(null);}}
    @EventHandler public void chat(AsyncPlayerChatEvent e){
        e.getRecipients().removeIf(viewer->p.mutedChat.contains(viewer.getUniqueId()));
    }
}
