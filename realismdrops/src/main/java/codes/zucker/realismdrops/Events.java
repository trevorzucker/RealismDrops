package codes.zucker.realismdrops;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.v1_15_R1.PacketPlayOutEntityDestroy;

public class Events implements Listener {

    public void pMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        for(Entity ent : p.getNearbyEntities(15, 15, 15)) {
            if (!(ent instanceof Item)) continue;
            Item i = (Item)ent;
            PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(i.getEntityId());
            ((CraftPlayer)p).getHandle().playerConnection.sendPacket(packet);
        }
    }

    @EventHandler
    public void pPickupItem(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player))
            return;
        ItemStack itemPickingUp = e.getItem().getItemStack();
        
        Player p = (Player)e.getEntity();
        List<Entity> list = p.getNearbyEntities(0.75f, 0.75f, 0.75f);
        for(Entity ent : list) {
            if (!(ent instanceof Item)) continue;
            Item item = (Item)ent;
            CollidableItem i = CollidableItem.GetFromItemEntity(item);
            if (i == null) continue;
            if (i.born + CollidableItem.TIME_UNTIL_CAN_PICKUP > System.currentTimeMillis()) continue;
            i.collect(p);
        }

        e.setCancelled(itemPickingUp.getType().equals(Material.DEBUG_STICK) && itemPickingUp.getAmount() == 51);
    }

    @EventHandler
    public void eSpawn(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof Item)) return;
        Item i = (Item)e.getEntity();

        if (i.getItemStack().getType().equals(Material.DEBUG_STICK) && i.getItemStack().getAmount() == 51) return;

        Location loc = i.getLocation().clone();
        loc.setDirection(i.getVelocity().normalize());

        CollidableItem item = new CollidableItem(loc, i.getItemStack());
        item.collision.setVelocity(i.getVelocity());
        item.RecalculateAngles();
        i.remove();
    }

}