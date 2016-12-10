/*******************************************************************************
 * This file is part of ASkyBlock.
 *
 *     ASkyBlock is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ASkyBlock is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.wasteofplastic.acidisland.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.scheduler.BukkitTask;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.wasteofplastic.acidisland.ASkyBlock;
import com.wasteofplastic.acidisland.Island;
import com.wasteofplastic.acidisland.Settings;

/**
 * @author tastybento
 * 
 */
public class LavaCheck implements Listener {
    BukkitTask task;
    private final ASkyBlock plugin;
    private final static boolean DEBUG = false;
    private final Random random;
    private final static List<BlockFace> FACES = Arrays.asList(BlockFace.SELF, BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);
    private static Multiset<Material> stats = HashMultiset.create();

    public LavaCheck(ASkyBlock aSkyBlock) {
        plugin = aSkyBlock;
        random = new Random();
        stats.clear();
    }

    /**
     * Removes stone generated by lava pouring onto water
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCleanstoneGen(BlockFromToEvent e) {
        // Only do this in ASkyBlock world
        if (!e.getBlock().getWorld().equals(ASkyBlock.getIslandWorld())) {
            return;
        }
        // Do nothing if a new island is being created
        if (plugin.isNewIsland())
            return;
        final Block to = e.getToBlock();
        /*
		plugin.getLogger().info("From material is " + e.getBlock().toString());
		plugin.getLogger().info("To material is " + to.getType().toString());
		plugin.getLogger().info("---------------------------------");
         */
        if (Settings.acidDamage > 0) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: cleanstone gen " + e.getEventName());

            final Material prev = to.getType();
            // plugin.getLogger().info("To material was " +
            // to.getType().toString());
            plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    // plugin.getLogger().info("To material is after 1 tick " +
                    // to.getType().toString());
                    if ((prev.equals(Material.WATER) || prev.equals(Material.STATIONARY_WATER)) && to.getType().equals(Material.STONE)) {
                        to.setType(prev);
                        if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                            to.getWorld().playSound(to.getLocation(), Sound.valueOf("FIZZ"), 1F, 2F);
                        } else {
                            to.getWorld().playSound(to.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1F, 2F);
                        }
                    }
                }
            });
        }
    }

    /**
     * Magic Cobble Generator
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCobbleGen(BlockFromToEvent e){
        /*
        plugin.getLogger().info("DEBUG: " + e.getEventName());
        plugin.getLogger().info("From material is " + e.getBlock().toString());
        plugin.getLogger().info("To material is " + e.getToBlock().getType().toString());
        plugin.getLogger().info("---------------------------------");
         */
        // If magic cobble gen isnt used
        if(!Settings.useMagicCobbleGen) {
            //plugin.getLogger().info("DEBUG: no magic cobble gen");
            return;
        }

        // Only do this in ASkyBlock world
        if (!e.getBlock().getWorld().equals(ASkyBlock.getIslandWorld())) {
            //plugin.getLogger().info("DEBUG: wrong world");
            return;
        }
        // Do nothing if a new island is being created
        if (plugin.isNewIsland()) {
            //plugin.getLogger().info("DEBUG: new island in creation");
            return;
        }

        final Block b = e.getBlock();
        if (b.getType().equals(Material.WATER) || b.getType().equals(Material.STATIONARY_WATER) 
                || b.getType().equals(Material.LAVA) || b.getType().equals(Material.STATIONARY_LAVA)) {
            //plugin.getLogger().info("DEBUG: From block is water or lava. To = " + e.getToBlock().getType());
            final Block toBlock = e.getToBlock();
            if (toBlock.getType().equals(Material.AIR) && generatesCobble(b, toBlock)){
                //plugin.getLogger().info("DEBUG: potential cobble gen");
                // Get island level or use default
                int l = Integer.MIN_VALUE;
                Island island = plugin.getGrid().getIslandAt(b.getLocation());
                if (island != null) {
                    if (island.getOwner() != null) {	                    
                        l = plugin.getPlayers().getIslandLevel(island.getOwner());
                        //plugin.getLogger().info("DEBUG: level " + level);
                    }
                }
                final int level = l;
                // Check if cobble was generated next tick
                // Store surrounding blocks and their current material types
                final List<Block> prevBlock = new ArrayList<Block>();
                final List<Material> prevMat = new ArrayList<Material>();
                for (BlockFace face: FACES) {
                    Block r = toBlock.getRelative(face);
                    prevBlock.add(r);
                    prevMat.add(r.getType());
                    //r = toBlock.getRelative(face,2);
                    //prevBlock.add(r);
                    //prevMat.add(r.getType());
                }
                // Check if they became cobblestone next tick
                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Iterator<Block> blockIt = prevBlock.iterator();
                        Iterator<Material> matIt = prevMat.iterator();
                        while (blockIt.hasNext() && matIt.hasNext()) {
                            Block block = blockIt.next();
                            Material material = matIt.next();
                            if (block.getType().equals(Material.COBBLESTONE) && !block.getType().equals(material)) {
                                //plugin.getLogger().info("DEBUG: " + material + " => " + block.getType());
                                //plugin.getLogger().info("DEBUG: Cobble generated. Island level = " + level);
                                if(!Settings.magicCobbleGenChances.isEmpty()){
                                    Entry<Integer,TreeMap<Double,Material>> entry = Settings.magicCobbleGenChances.floorEntry(level);
                                    double maxValue = entry.getValue().lastKey();                                    
                                    double rnd = random.nextDouble() * maxValue;
                                    Entry<Double, Material> en = entry.getValue().ceilingEntry(rnd);
                                    //plugin.getLogger().info("DEBUG: Cobble generated. Island level = " + level);
                                    //plugin.getLogger().info("DEBUG: rnd = " + rnd + "/" + maxValue);
                                    //plugin.getLogger().info("DEBUG: material = " + en.getValue());
                                    if (en != null) {
                                        block.setType(en.getValue());
                                        // Record stats
                                        stats.add(en.getValue());
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }
    }


    public boolean generatesCobble(Block block, Block toBlock){
        Material mirrorID1 = (block.getType().equals(Material.WATER)) || (block.getType().equals(Material.STATIONARY_WATER)) ? Material.LAVA : Material.WATER;
        Material mirrorID2 = (block.getType().equals(Material.WATER)) || (block.getType().equals(Material.STATIONARY_WATER)) ? Material.STATIONARY_LAVA : Material.STATIONARY_WATER;
        for (BlockFace face: FACES) {
            Block r = toBlock.getRelative(face);
            if ((r.getType().equals(mirrorID1)) || (r.getType().equals(mirrorID2))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the magic cobble gen stats
     */
    public static Multiset<Material> getStats() {
        return stats;
    }

    /**
     * Clears the magic cobble gen stats
     */
    public static void clearStats() {
        stats.clear();
    }
}

// Failed attempts - remember the pain
// Not this event
/*
 * @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 * public void onStone(BlockFormEvent e) {
 * plugin.getLogger().info(e.getEventName());
 * }
 */
/*
 * @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 * public void onStone(BlockPhysicsEvent e) {
 * plugin.getLogger().info(e.getEventName());
 * plugin.getLogger().info("DEBUG: block physics " +
 * e.getBlock().getType());
 * plugin.getLogger().info("DEBUG: block physics changed " +
 * e.getChangedType());
 * plugin.getLogger().info("---------------------------------");
 * if (e.getChangedType().equals(Material.WATER) ||
 * e.getChangedType().equals(Material.STATIONARY_WATER)
 * && e.getBlock().getType().equals(Material.STONE)) {
 * e.getBlock().setType(Material.WATER);
 * e.getBlock().getWorld().playSound(e.getBlock().getLocation(), Sound.FIZZ,
 * 1F, 1F);
 * }
 * }
 */
/*
 * @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
 * public void onStone(BlockSpreadEvent e) {
 * plugin.getLogger().info(e.getEventName());
 * }
 */
/*
 * if ((from.equals(Material.STATIONARY_WATER) &&
 * to.getType().equals(Material.STONE))
 * || (from.equals(Material.STATIONARY_LAVA) &&
 * to.getType().equals(Material.STATIONARY_WATER))
 * || (from.equals(Material.LAVA) &&
 * to.getType().equals(Material.STATIONARY_WATER))) {
 * // plugin.getLogger().info("from sw to st cancelled");
 * // to.setType(Material.FIRE);
 * to.setType(Material.STATIONARY_WATER);
 * e.getBlock().getWorld().playSound(e.getBlock().getLocation(), Sound.FIZZ,
 * 1F, 1F);
 * e.setCancelled(true);
 * //return;
 * }
 * // Get the from block
 * Block fromBlock = to.getRelative(oppositeFace(e.getFace()));
 * plugin.getLogger().info("DEBUG: From block " + fromBlock.getType() +
 * " location " + fromBlock.getX() + "," + fromBlock.getZ());
 * plugin.getLogger().info("To material before " + to.getType().toString());
 * final Material prev = to.getType();
 * plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
 * @Override
 * public void run() {
 * plugin.getLogger().info("To material is after 1 tick " +
 * to.getType().toString());
 * if ((prev.equals(Material.WATER) ||
 * prev.equals(Material.STATIONARY_WATER)) &&
 * to.getType().equals(Material.STONE)) {
 * to.setType(prev);
 * to.getWorld().playSound(to.getLocation(), Sound.FIZZ, 1F, 1F);
 * }
 * }});
 * if ((from.equals(Material.STATIONARY_WATER) ||
 * from.equals(Material.WATER))) {
 * // Look around the from block
 * for (BlockFace bf: BlockFace.values()) {
 * switch (bf) {
 * case DOWN:
 * case EAST:
 * case NORTH:
 * case NORTH_EAST:
 * case NORTH_WEST:
 * case SOUTH:
 * case SOUTH_EAST:
 * case SOUTH_WEST:
 * case UP:
 * case WEST:
 * Block adjacent = fromBlock.getRelative(bf);
 * if (adjacent.getType().equals(Material.STONE)) {
 * adjacent.setType(Material.AIR);
 * adjacent.getWorld().playSound(e.getBlock().getLocation(), Sound.FIZZ, 1F,
 * 1F);
 * plugin.getLogger().info("DEBUG: Melting block " + adjacent.getType() +
 * " location " + adjacent.getX() + "," + adjacent.getZ());
 * }
 * break;
 * default:
 * break;
 * }
 * }
 * }
 */
/*
 * private BlockFace oppositeFace(BlockFace face) {
 * switch (face) {
 * case DOWN:
 * return BlockFace.UP;
 * case EAST:
 * return BlockFace.WEST;
 * case EAST_NORTH_EAST:
 * return BlockFace.WEST_SOUTH_WEST;
 * case EAST_SOUTH_EAST:
 * return BlockFace.WEST_NORTH_WEST;
 * case NORTH:
 * return BlockFace.SOUTH;
 * case NORTH_EAST:
 * return BlockFace.SOUTH_WEST;
 * case NORTH_NORTH_EAST:
 * return BlockFace.SOUTH_SOUTH_WEST;
 * case NORTH_NORTH_WEST:
 * return BlockFace.SOUTH_SOUTH_EAST;
 * case NORTH_WEST:
 * return BlockFace.SOUTH_EAST;
 * case SELF:
 * return BlockFace.SELF;
 * case SOUTH:
 * return BlockFace.NORTH;
 * case SOUTH_EAST:
 * return BlockFace.NORTH_WEST;
 * case SOUTH_SOUTH_EAST:
 * return BlockFace.NORTH_NORTH_WEST;
 * case SOUTH_SOUTH_WEST:
 * return BlockFace.NORTH_NORTH_EAST;
 * case SOUTH_WEST:
 * return BlockFace.NORTH_EAST;
 * case UP:
 * return BlockFace.DOWN;
 * case WEST:
 * return BlockFace.EAST;
 * case WEST_NORTH_WEST:
 * return BlockFace.EAST_SOUTH_EAST;
 * case WEST_SOUTH_WEST:
 * return BlockFace.EAST_NORTH_EAST;
 * default:
 * return BlockFace.SELF;
 * }
 * }
 */
