package codes.zucker.realismdrops;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Consumer;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_15_R1.PacketPlayOutEntityDestroy;


public class CollidableItem {
    public static List<CollidableItem> list = new ArrayList<CollidableItem>();
    final static double TIME_UNTIL_CAN_PICKUP = 2000;

    public Item collision;
    ArmorStand item;
    Vector offset = new Vector(0, -0.675f, 0);
    public double born;

    final static String[] TOOLS_MATERIALS = new String[] {
        "SWORD",
        "AXE",
        "SHOVEL",
        "HOE",
        "PICKAXE",
        "BOW",
        "TRIDENT",
        "FISHING",
        "STICK",
    };

    public CollidableItem(Location spawn, ItemStack item) {
        this.item = spawn.getWorld().spawn(spawn.clone().add(offset), ArmorStand.class, new Consumer<ArmorStand>() {
            @Override
            public void accept(ArmorStand t) {
                t.setInvulnerable(true);
                t.setVisible(false);
                t.setMarker(true);
                t.setArms(true);
                t.setSilent(true);
                t.setBasePlate(false);
                t.setGravity(false);
                EntityEquipment standEquipment = t.getEquipment();
                standEquipment.setItemInMainHand(item);
                t.setRightArmPose(new EulerAngle(Math.toRadians(0), 0, 0));
            }
        });

        this.collision = spawn.getWorld().spawn(spawn.clone().add(new Vector(50, 50, 50)), Item.class, new Consumer<Item>() {
            @Override
            public void accept(Item t) {
                t.teleport(spawn.clone().add(new Vector(50, 50, 50)));
                t.setInvulnerable(true);
                t.setSilent(true);
                t.setGravity(true);
                t.teleport(spawn);
                t.setItemStack(new ItemStack(Material.DEBUG_STICK, 51));
            }
        });
        
        for(Entity ent : this.item.getNearbyEntities(15, 15, 15)) {
            if (!(ent instanceof Player)) continue;
            Player p = (Player)ent;
            PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(collision.getEntityId());
            ((CraftPlayer)p).getHandle().playerConnection.sendPacket(packet);
        }

        setArmPitch(-90);
        list.add(this);
        born = System.currentTimeMillis();
    }

    public void teleport(Location newLoc) {
        collision.teleport(newLoc);
        item.teleport(newLoc.clone().add(offset));
    }

    public double getArmPitch() {
        return Math.toDegrees(item.getRightArmPose().getX());
    }

    public void setArmPitch(double deg) {
        if (!list.contains(this)) return;
        List<Entity> near = collision.getNearbyEntities(0.75f, 0.75f, 0.75f);
        for(Entity entry : near) {
            if (!(entry instanceof ArmorStand))
                continue;

            ArmorStand entryStand = (ArmorStand)entry;

            if(entryStand.equals(item)) continue;

            EntityEquipment equip = entryStand.getEquipment();
            EntityEquipment thisEquip = item.getEquipment();
            ItemStack equipItem = equip.getItemInMainHand();
            ItemStack thisEquipItem = thisEquip.getItemInMainHand();

            if (equipItem.getType().equals(thisEquipItem.getType()) && entryStand.isMarker()) {
                int newAmount = thisEquipItem.getAmount() + equipItem.getAmount();
                ItemStack temp = new ItemStack(equipItem.getType(), newAmount);
                temp.setData(equipItem.getData());
                equip.setItemInMainHand(temp);
                temp = null;
                collision.remove();
                item.remove();
                list.remove(this);
                return;
            }
                
        }

        boolean isBlock = item.getEquipment().getItemInMainHand().getType().isSolid() && item.getEquipment().getItemInMainHand().getType().isBlock();
        for(String s : NOT_BLOCK_MATERIALS) {
            if (item.getEquipment().getItemInMainHand().getType().name().equals(s)) {
                isBlock = false;
                break;
            }
        }
        deg = isBlock ? Utils.clamp(deg, -90, -16) : deg;

        boolean isTool = false;
        for(String s : TOOLS_MATERIALS) {
            if (item.getEquipment().getItemInMainHand().getType().toString().contains(s) && !item.getEquipment().getItemInMainHand().getType().toString().contains("CROSSBOW")) {
                isTool = true;
                break;
            }
        }

        item.setRightArmPose(new EulerAngle(Math.toRadians(deg) + (item.getEquipment().getItemInMainHand().getType().toString().contains("CROSSBOW") ? Math.toRadians(-90) : 0), 0, isTool ? Math.toRadians(-90) : 0));
        offset = new Vector(0, -0.35, 0);
        double armDegY = Math.abs(deg);
        armDegY = armDegY > 180 ? armDegY - 180 : armDegY;
        double yOffset = armDegY * 0.002805d;
        if (collision.isOnGround())
            yOffset = 0.45f;
        offset.add(new Vector(0, -yOffset, 0));

        double armDeg = deg;

        armDeg = (Math.abs(deg) > 90) ? armDeg - 90 : armDeg;
        double armOffset = (-armDeg % 180);
        armOffset = armOffset / (-armDeg / 0.345f);
        Vector itemFwdNormalized = collision.getLocation().getDirection().normalize();
        Vector itemFwd = new Vector(itemFwdNormalized.getX(), 0, itemFwdNormalized.getZ());
        Vector itemFwdOffset = itemFwd.clone();
        itemFwdOffset.multiply(-1); itemFwdOffset.multiply(0.25f + armOffset); // zero is .25, fully extended .55
        offset.add(itemFwdOffset);
        Vector left = new Vector(itemFwdNormalized.getZ(), 0, -itemFwdNormalized.getX());
        offset.add(left.multiply(0.4f));

        if (isBlock)
            offset.add(new Vector(0, 0.2f, 0));
        if(item.getEquipment().getItemInMainHand().getType().name().contains("CARPET"))
            offset.add(new Vector(0, 0.1f, 0));
        if (isTool || item.getEquipment().getItemInMainHand().getType().toString().contains("CROSSBOW"))
            offset.add(new Vector(0, -0.5f, 0).subtract(left));

        item.teleport(collision.getLocation().clone().add(offset));
    }

    final static String[] NOT_BLOCK_MATERIALS = new String[] { 
        "HOPPER",
        "CAMPFIRE",
        "LANTERN",
        "IRON_BARS",
        "PANE",
        "BELL",
        "CAKE",
        "PUMPKIN_PIE",
        "BREWING_STAND",
        "CAULDRON"
    };

    public void RecalculateAngles() {
        if (collision.getLocation() == null) {
            item.remove();
            list.remove(this);
            return;
        }
        Vector normalizedVelocity = collision.getVelocity().normalize();
        Vector velocity = collision.getVelocity();
        velocity = new Vector(velocity.getX(), velocity.getY() + 0.0784000015258789d, velocity.getZ());
        final Vector finalVelocity = velocity;

        boolean isBlock = item.getEquipment().getItemInMainHand().getType().isSolid() && item.getEquipment().getItemInMainHand().getType().isBlock();
        for(String s : NOT_BLOCK_MATERIALS) {
            if (item.getEquipment().getItemInMainHand().getType().name().equals(s)) {
                isBlock = false;
                break;
            }
        }
        final boolean finalIsBlock = isBlock;

        boolean isTool = false;
        for(String s : TOOLS_MATERIALS) {
            if (item.getEquipment().getItemInMainHand().getType().toString().contains(s)) {
                isTool = true;
                break;
            }
        }
        final boolean finalIsTool = isTool;

        if (velocity.length() > 0.01f || !collision.isOnGround()) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(Main.class), new Runnable(){
        
                @Override
                public void run() {
                    if (finalVelocity.equals(new Vector(0, 0, 0))) return;
                    //setArmPitch();
                    double angle = collision.getLocation().getDirection().normalize().dot(finalVelocity);
                    angle = finalIsBlock ? angle * 2 : angle;
                    setArmPitch(-90 * angle);
                    RecalculateAngles();
                }
            }, 1);
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(Main.class), new Runnable(){
                @Override
                public void run() {
                    setArmPitch(finalIsBlock ? -16 : 0.0001d);
                    Location offset = item.getLocation().clone();
                    double distance = collision.getLocation().toVector().getY() - offset.toVector().getY();
                    offset.add(new Vector(0, distance - 0.75f, 0));
                    if (finalIsBlock)
                        offset.add(new Vector(0, 0.2f, 0));
                    if(item.getEquipment().getItemInMainHand().getType().name().contains("CARPET") || item.getEquipment().getItemInMainHand().getType().name().contains("SNOW"))
                        offset.add(new Vector(0, 0.1f, 0));
                    if (finalIsTool)
                        offset.add(new Vector(0, -0.5f, 0));            
        
                    item.teleport(offset);
                }
            }, 1);
        }
    }

    public void collect(Player p) {
        PlayerInventory inv = p.getInventory();
        inv.addItem(item.getEquipment().getItemInMainHand());
        Random r = new Random();
        p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1 + r.nextFloat() / 2);
        collision.remove();
        item.remove();
        list.remove(this);
    }

    public static CollidableItem GetFromArmorStand(ArmorStand stand) {
        for(CollidableItem item : list) {
            if (stand.equals(item.item))
                return item;
        }
        return null;
    }

    public static CollidableItem GetFromSilverfish(Silverfish fish) {
        for(CollidableItem item : list) {
            if (fish.equals(item.collision))
                return item;
        }
        return null;
    }

    public static CollidableItem GetFromItemEntity(Item item) {
        for(CollidableItem _item : list) {
            if (item.equals(_item.collision))
                return _item;
        }
        return null;
    }
}