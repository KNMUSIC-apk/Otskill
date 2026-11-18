package com.knmusic.otmitems;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Theo dõi các khối đã bị đổi thành băng cùng khối gốc để khôi phục sau đó.
 * Mỗi vị trí chỉ được một nguồn giữ tại một thời điểm.
 */
public final class IceBlocks {

    private record Cell(UUID world, int x, int y, int z) {
    }

    private final PluginConfig config;
    private final Map<Cell, BlockData> originals = new HashMap<>();

    public IceBlocks(PluginConfig config) {
        this.config = config;
    }

    /**
     * Đổi khối thành băng. Trả về false nếu khối đã được theo dõi,
     * nằm trong danh sách bảo vệ, hoặc là block entity (rương, biển...).
     */
    public boolean place(Block block) {
        if (config.isProtectedBlock(block.getType())) {
            return false;
        }
        if (block.getState() instanceof TileState) {
            return false;
        }
        Cell cell = cellOf(block);
        if (originals.containsKey(cell)) {
            return false;
        }
        originals.put(cell, block.getBlockData());
        block.setType(config.getIceMaterial(), false);
        return true;
    }

    /** Khôi phục khối gốc nếu khối này đang được theo dõi. */
    public void restore(Block block) {
        BlockData original = originals.remove(cellOf(block));
        if (original != null) {
            block.setBlockData(original, false);
        }
    }

    /** Khôi phục toàn bộ khối gốc, dùng khi tắt plugin. */
    public void restoreAll() {
        for (Map.Entry<Cell, BlockData> entry : originals.entrySet()) {
            Cell cell = entry.getKey();
            World world = Bukkit.getWorld(cell.world());
            if (world != null) {
                world.getBlockAt(cell.x(), cell.y(), cell.z()).setBlockData(entry.getValue(), false);
            }
        }
        originals.clear();
    }

    private static Cell cellOf(Block block) {
        return new Cell(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }
}
