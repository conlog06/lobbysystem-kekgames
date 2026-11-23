package de.sujuc.lobby;

import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

final class Effects {
    private final LobbyPlugin p;
    private final Map<UUID,ArmorStand> balloons=new HashMap<>();
    private final Map<UUID,String> colors=new HashMap<>();
    private double phase;
    Effects(LobbyPlugin plugin){p=plugin;}
    void remove(UUID id){ArmorStand a=balloons.remove(id);if(a!=null)a.remove();colors.remove(id);}
    void balloon(LobbyPlugin.Session s){
        String selected=s.profile.balloon();UUID id=s.player.getUniqueId();
        if(selected.isEmpty()||p.builders.contains(id)){remove(id);return;}
        ArmorStand old=balloons.get(id);if(selected.equals(colors.get(id))&&old!=null&&old.isValid())return;
        remove(id);ArmorStand a=s.player.getWorld().spawn(s.player.getLocation().add(.8,1.5,.8),ArmorStand.class);
        a.setVisible(false);a.setGravity(false);a.setSmall(true);a.setMarker(true);a.setInvulnerable(true);a.setSilent(true);a.setPersistent(false);
        a.getEquipment().setHelmet(new ItemStack(Material.valueOf(selected)));a.getPersistentDataContainer().set(p.npcKey,org.bukkit.persistence.PersistentDataType.STRING,"balloon");
        balloons.put(id,a);colors.put(id,selected);
    }
    void tick(){phase+=.22;for(LobbyPlugin.Session s:p.sessions.values()){
        Player player=s.player;if(s.profile==null||!p.lobby(player)||p.builders.contains(player.getUniqueId()))continue;
        if(!s.profile.particle().isEmpty()){
            Location at=player.getLocation().add(Math.cos(phase)*.6,.2,Math.sin(phase)*.6);
            try{player.getWorld().spawnParticle(Particle.valueOf(s.profile.particle()),at,2,.05,.05,.05,0);}catch(IllegalArgumentException ignored){}
        }
        balloon(s);ArmorStand a=balloons.get(player.getUniqueId());if(a!=null){
            a.teleport(player.getLocation().add(.8,1.4+Math.sin(phase)*.12,.8));
            // Respect the viewer's visibility preference for companion entities as well.
            for(LobbyPlugin.Session viewer:p.sessions.values())if(viewer.player.canSee(player))viewer.player.showEntity(p,a);else viewer.player.hideEntity(p,a);
        }
    }}
    void gadget(LobbyPlugin.Session s){
        Player player=s.player;String type=s.profile.gadget();if(type.isEmpty()){p.menus.open(player,"gadgets");return;}
        long now=System.currentTimeMillis();long wait=s.gadgetAt+p.cfg.gadgetCooldownSeconds*1000L-now;
        if(wait>0){p.ui.chat(player,p.cfg.prefix+"&eWarte noch "+((wait+999)/1000)+" Sekunden.");return;}s.gadgetAt=now;
        switch(type){
            case "DASH"->player.setVelocity(player.getLocation().getDirection().multiply(1.6).setY(.5));
            case "GRAPPLING"->{org.bukkit.block.Block target=player.getTargetBlockExact(20);if(target==null)return;Vector direction=target.getLocation().add(.5,1.5,.5).toVector().subtract(player.getLocation().toVector());if(direction.lengthSquared()>0)player.setVelocity(direction.normalize().multiply(1.6).setY(.75));}
            case "PEARL"->{
                Location target=player.getLocation().clone();Vector step=target.getDirection().normalize().multiply(.5);
                for(int i=0;i<24;i++){Location next=target.clone().add(step);if(!next.getBlock().isPassable()||!next.clone().add(0,1,0).getBlock().isPassable()||!next.getWorld().getWorldBorder().isInside(next))break;target=next;}
                player.teleport(target);player.getWorld().spawnParticle(Particle.PORTAL,target,45,.5,.6,.5,.05);
            }
            case "FIREWORK"->player.getWorld().spawnParticle(Particle.FIREWORK,player.getEyeLocation().add(player.getLocation().getDirection().multiply(2)),60,.5,.5,.5,.12);
            case "SNOW"->player.getWorld().spawnParticle(Particle.SNOWFLAKE,player.getEyeLocation().add(player.getLocation().getDirection().multiply(2)),80,1,1,1,.05);
            default->{return;}
        }
        player.playSound(player.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,.5f,1.5f);
    }
}
