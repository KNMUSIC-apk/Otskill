package com.knmusic.otmitems;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tạo vùng băng quanh người dùng skill ngay lập tức. Hết thời gian, băng tan dần từ ngoài vào tâm,
 * mỗi vòng tan sẽ khôi phục khối gốc của vòng đó.
 */
public final class IceFieldManager {

    private final OTMitems plugin;
    private final IceBlocks iceBlocks;

    public IceFieldManager(OTMitems plugin, IceBlocks iceBlocks) {
        this.plugin = plugin;
        this.iceBlocks = iceBlocks;
    }

    public void create(Player caster, SkillDefinition skill) {
        SkillDefinition.IceField field = skill.ice();
        if (field == null) {
            return;
        }

        Block center = caster.getLocation().getBlock();
        BoundingBox casterBox = caster.getBoundingBox();
        int r = field.radius();
        int rSq = r * r;

        // Khoảng cách làm tròn -> danh sách khối, xếp từ xa đến gần để tan từ ngoài vào
        Map<Integer, List<Block>> rings = new TreeMap<>(Comparator.reverseOrder());

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > rSq) {
                        continue;
                    }
                    Block block = center.getRelative(dx, dy, dz);
                    // Không biến ô mà chính người dùng đang chiếm chỗ thành băng
                    if (casterBox.overlaps(cellBox(block))) {
                        continue;
                    }
                    if (iceBlocks.place(block)) {
                        int ring = (int) Math.round(Math.sqrt(distSq));
                        rings.computeIfAbsent(ring, k -> new ArrayList<>()).add(block);
                    }
                }
            }
        }

        if (rings.isEmpty()) {
            return;
        }

        Location centerLoc = center.getLocation().add(0.5, 0.5, 0.5);
        World world = center.getWorld();
        world.playSound(centerLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
        world.spawnParticle(Particle.SNOWFLAKE, centerLoc, 60, r, r * 0.5, r, 0.02);

        new MeltTask(new ArrayList<>(rings.values()))
                .runTaskTimer(plugin, field.durationTicks(), field.meltRingTicks());
    }

    private static BoundingBox cellBox(Block block) {
        return new BoundingBox(block.getX(), block.getY(), block.getZ(),
                block.getX() + 1, block.getY() + 1, block.getZ() + 1);
    }

    /** Tan băng theo từng vòng, vòng ngoài cùng tan trước. */
    private final class MeltTask extends BukkitRunnable {

        private final List<List<Block>> rings;
        private int next;

        MeltTask(List<List<Block>> rings) {
            this.rings = rings;
        }

        @Override
        public void run() {
            if (next >= rings.size()) {
                cancel();
                return;
            }
            for (Block block : rings.get(next++)) {
                iceBlocks.restore(block);
                block.getWorld().spawnParticle(Particle.DRIPPING_WATER,
                        block.getLocation().add(0.5, 0.5, 0.5), 2, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }
}
