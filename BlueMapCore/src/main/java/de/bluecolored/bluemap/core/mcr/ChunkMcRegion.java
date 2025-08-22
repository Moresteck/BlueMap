/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.mcr;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.LightData;
import net.querz.nbt.CompoundTag;

@SuppressWarnings("FieldMayBeFinal")
public class ChunkMcRegion extends MCRChunk {
    private static final long[] EMPTY_LONG_ARRAY = new long[0];

    private boolean isGenerated;
    private boolean hasLight;
    private Section section;

    @SuppressWarnings("unchecked")
    public ChunkMcRegion(MCRWorld world, CompoundTag chunkTag) {
        super(world, chunkTag);

        CompoundTag levelData = chunkTag.getCompoundTag("Level");

        this.isGenerated = levelData.getBoolean("TerrainPopulated");
        this.hasLight = isGenerated;

        section = new Section(levelData, world);
    }

    @Override
    public boolean isGenerated() {
        return isGenerated;
    }

    @Override
    public long getInhabitedTime() {
        return 1;
    }
    
    @Override
    public int fromBlocksArray(int x, int y, int z) {
    	x &= 0xF; z &= 0xF;
    	
    	return this.section.blocks[x << 11 | z << 7 | y] & 255;
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        if (y >= 128 || y <= 0)
        	return BlockState.AIR;

        if (this.section == null)
        	return BlockState.AIR;

        return this.section.getBlockState(x, y, z);
    }

    @Override
    public LightData getLightData(int x, int y, int z, LightData target) {
        if (!hasLight) return target.set(getWorld().getSkyLight(), 0);

        if (y >= 128 || y <= 0)
            return (y < 0) ? target.set(0, 0) : target.set(getWorld().getSkyLight(), 0);

        if (this.section == null)
        	return target.set(getWorld().getSkyLight(), 0);

        return this.section.getLightData(x, y, z, target);
    }

    @Override
    public String getBiome(int x, int y, int z) {
        return LegacyBiomes.idFor(this.getWorld().wcm.getBiome(x, z));
    }

    @Override
    public int getWorldSurfaceY(int x, int z) {
        return 63;
    }

    @Override
    public int getOceanFloorY(int x, int z) {
        return 50; // TODO figure out the actual min noise value
    }

    private static class Section {
        private static final int AIR_ID = 0;

        private NibbleArray blockLight;
        private NibbleArray skyLight;
        private NibbleArray metadata;
        protected byte[] blocks;
        //protected ListTag tileentities;
        private MCRWorld world;

        public Section(CompoundTag sectionData, MCRWorld world) {
        	this.world = world;
            this.blockLight = new NibbleArray(sectionData.getByteArray("BlockLight"));
            this.skyLight = new NibbleArray(sectionData.getByteArray("SkyLight"));
            this.metadata = new NibbleArray(sectionData.getByteArray("Data"));
            this.blocks = sectionData.getByteArray("Blocks");
            //this.tileentities = sectionData.getListTag("TileEntities");

            if (blocks.length < 256 && blocks.length > 0) blocks = Arrays.copyOf(blocks, 256);
            if (metadata.data.length < 256 && metadata.data.length > 0) metadata.data = Arrays.copyOf(metadata.data, 256);
            if (blockLight.data.length < 2048 && blockLight.data.length > 0) blockLight.data = Arrays.copyOf(blockLight.data, 2048);
            if (skyLight.data.length < 2048 && skyLight.data.length > 0) skyLight.data = Arrays.copyOf(skyLight.data, 2048);
        }

        public int getSectionY() {
            throw new RuntimeException("Method ChunkMcRegion.getSectionY() is unimplemented");
        }

        public BlockState getBlockState(int x, int y, int z) {
            if (blocks.length == 0) return BlockState.AIR;
            
            int ox = x, oz = z;

            x &= 0xF; z &= 0xF; // Math.floorMod(pos.getX(), 16)
            
            int block_id = this.blocks[x << 11 | z << 7 | y] & 255;
            int metadata = this.metadata.getData(x, y, z);
            
            if (block_id == AIR_ID)
            	return BlockState.AIR;
            
            BlockID bid = BlockID.query(block_id, metadata);
            
            if (bid == null)
            	bid = BlockID.query(block_id);
            
            if (bid == null)
            	return BlockState.MISSING;
            
            Map<String, String> metadataToProperties = BlockID.metadataToProperties(bid, metadata);
            
            // ugly patches
            if (block_id == 2) {
            	// if grass block, define whether it's snowy or not
                // (doesn't seem to affect performance much)
            	
            	int block_id_above = 0;
            	
            	if (y + 1 < 128) // avoid out of bounds
            		block_id_above = this.blocks[x << 11 | z << 7 | (y+1)] & 255;
            	
            	if (block_id_above == 78 || block_id_above == 80)
            		metadataToProperties.put("snowy", "true");
            	else
            		metadataToProperties.put("snowy", "false");
            	
            } else if (block_id == 90) {
            	// handle portals
            	
            	int block_id_xmin = this.world.getChunkAtBlock(ox-1, y, oz).fromBlocksArray(ox-1, y, oz);
            	int block_id_xplus = this.world.getChunkAtBlock(ox+1, y, oz).fromBlocksArray(ox+1, y, oz);
            	
            	if (block_id_xmin == 90 || block_id_xplus == 90)
            		metadataToProperties.put("axis", "x");
            	else
            		metadataToProperties.put("axis", "z");
            	
            } else if (block_id == 85) {
            	// handle fences
            	
            	int block_id_xmin = this.world.getChunkAtBlock(ox-1, y, oz).fromBlocksArray(ox-1, y, oz);
            	int block_id_xplus = this.world.getChunkAtBlock(ox+1, y, oz).fromBlocksArray(ox+1, y, oz);
            	int block_id_zmin = this.world.getChunkAtBlock(ox, y, oz-1).fromBlocksArray(ox, y, oz-1);
            	int block_id_zplus = this.world.getChunkAtBlock(ox, y, oz+1).fromBlocksArray(ox, y, oz+1);
            	
            	if (block_id_xmin == 85)
            		metadataToProperties.put("west", "true");
            	
            	if (block_id_xplus == 85)
            		metadataToProperties.put("east", "true");
            	
            	if (block_id_zmin == 85)
            		metadataToProperties.put("north", "true");
            	
            	if (block_id_zplus == 85)
            		metadataToProperties.put("south", "true");
            	
            } else if (block_id == 54) {
            	// handle chests <pain>
            	
            	int block_id_xmin = this.world.getChunkAtBlock(ox-1, y, oz).fromBlocksArray(ox-1, y, oz);
            	int block_id_xplus = this.world.getChunkAtBlock(ox+1, y, oz).fromBlocksArray(ox+1, y, oz);
            	int block_id_zmin = this.world.getChunkAtBlock(ox, y, oz-1).fromBlocksArray(ox, y, oz-1);
            	int block_id_zplus = this.world.getChunkAtBlock(ox, y, oz+1).fromBlocksArray(ox, y, oz+1);
            	
            	if (block_id_xmin == 54) {
            		
            		int block_id_xmin_zplus = this.world.getChunkAtBlock(ox-1, y, oz+1).fromBlocksArray(ox-1, y, oz+1);
            		int block_id_x_zplus = this.world.getChunkAtBlock(ox, y, oz+1).fromBlocksArray(ox, y, oz+1);
            		
            		if (BlockID.isOpaque(block_id_xmin_zplus) || BlockID.isOpaque(block_id_x_zplus)) {
            			
            			metadataToProperties.put("facing", "north");
            			metadataToProperties.put("type", "right");
            			
            			
            		} else {
            			
            			metadataToProperties.put("facing", "south");
            			metadataToProperties.put("type", "left");
            			
            		}
            	} else if (block_id_xplus == 54) {
            		
            		int block_id_xplus_zplus = this.world.getChunkAtBlock(ox+1, y, oz+1).fromBlocksArray(ox+1, y, oz+1);
            		int block_id_x_zplus = this.world.getChunkAtBlock(ox, y, oz+1).fromBlocksArray(ox, y, oz+1);
            		
            		if (BlockID.isOpaque(block_id_xplus_zplus) || BlockID.isOpaque(block_id_x_zplus)) {
            			
            			metadataToProperties.put("facing", "north");
            			metadataToProperties.put("type", "left");
            			
            		} else {
            			
            			metadataToProperties.put("facing", "south");
            			metadataToProperties.put("type", "right");
            			
            		}
            	} else if (block_id_zmin == 54) {
            		
            		int block_id_zmin_xplus = this.world.getChunkAtBlock(ox+1, y, oz-1).fromBlocksArray(ox+1, y, oz-1);
            		int block_id_z_xplus = this.world.getChunkAtBlock(ox+1, y, oz).fromBlocksArray(ox+1, y, oz);
            		
            		if (BlockID.isOpaque(block_id_zmin_xplus) || BlockID.isOpaque(block_id_z_xplus)) {
            			
            			metadataToProperties.put("facing", "west");
            			metadataToProperties.put("type", "left");
            			
            		} else {
            			
            			metadataToProperties.put("facing", "east");
            			metadataToProperties.put("type", "right");
            			
            		}
            	} else if (block_id_zplus == 54) {
            		
            		int block_id_zplus_xplus = this.world.getChunkAtBlock(ox+1, y, oz+1).fromBlocksArray(ox+1, y, oz+1);
            		int block_id_z_xplus = this.world.getChunkAtBlock(ox+1, y, oz).fromBlocksArray(ox+1, y, oz);
            		
            		
            		if (BlockID.isOpaque(block_id_zplus_xplus) || BlockID.isOpaque(block_id_z_xplus)) {
            			
            			metadataToProperties.put("facing", "west");
            			metadataToProperties.put("type", "right");
            			
            		} else {
            			
            			metadataToProperties.put("facing", "east");
            			metadataToProperties.put("type", "left");
            			
            		}
            	} else {
            		// singular chest
            		
            		metadataToProperties.put("type", "single");
            		
            		if (BlockID.isOpaque(block_id_zmin))
            			metadataToProperties.put("facing", "south");
            		else if (BlockID.isOpaque(block_id_xmin))
            			metadataToProperties.put("facing", "east");
            		else if (BlockID.isOpaque(block_id_zplus))
            			metadataToProperties.put("facing", "north");
            		else if (BlockID.isOpaque(block_id_xplus))
            			metadataToProperties.put("facing", "west");
            		else
            			metadataToProperties.put("facing", "south");
            	}
            } else if (block_id == 64 || block_id == 71) {
            	// handle doors
            	
            	// the hinge is always on the left. right-hinge doors are just of different facing
            	metadataToProperties.put("hinge", "left");
            	metadataToProperties.put("powered", "false");
            	
            	if (metadata < 8)
            		metadataToProperties.put("half", "lower");
            	else
            		metadataToProperties.put("half", "upper");
            	
            	
            	metadata %= 8;
            	
            	if (metadata < 4)
            		metadataToProperties.put("open", "false");
            	else
            		metadataToProperties.put("open", "true");
            	
            	metadata %= 4;
            	
            	if (metadata == 0)
    				metadataToProperties.put("facing", "east");
    			else if (metadata == 1)
    				metadataToProperties.put("facing", "south");
    			else if (metadata == 2)
    				metadataToProperties.put("facing", "west");
    			else if (metadata == 3)
    				metadataToProperties.put("facing", "north");
            	
            	
            } else if (block_id == 51) {
            	// handle fire
            	
            	int block_id_below = 0;
            	
            	if (y - 1 >= 0) // avoid out of bounds
            		block_id_below = this.blocks[x << 11 | z << 7 | (y-1)] & 255;
            	
            	if (BlockID.isOpaque(block_id_below) || block_id_below == 30 || block_id_below == 52 ||
            			block_id_below == 85) { // + web, spawner, fence
            		
            		metadataToProperties.put("west", "false");
            		metadataToProperties.put("east", "false");
            		metadataToProperties.put("north", "false");
            		metadataToProperties.put("south", "false");
            		metadataToProperties.put("up", "false");
            		
            	} else {
            		
            		if (y + 1 < 128) { // avoid out of bounds
            			
            			int block_id_above = this.blocks[x << 11 | z << 7 | (y+1)] & 255;
            			
            			if (BlockID.isFlammable(block_id_above))
                    		metadataToProperties.put("up", "true");
            			
            		}
            		
            		int block_id_xmin = this.world.getChunkAtBlock(ox-1, y, oz).fromBlocksArray(ox-1, y, oz);
                	int block_id_xplus = this.world.getChunkAtBlock(ox+1, y, oz).fromBlocksArray(ox+1, y, oz);
                	int block_id_zmin = this.world.getChunkAtBlock(ox, y, oz-1).fromBlocksArray(ox, y, oz-1);
                	int block_id_zplus = this.world.getChunkAtBlock(ox, y, oz+1).fromBlocksArray(ox, y, oz+1);
                	
                	if (BlockID.isFlammable(block_id_xmin))
                		metadataToProperties.put("west", "true");
                	
                	if (BlockID.isFlammable(block_id_zmin))
                		metadataToProperties.put("north", "true");
                	
                	if (BlockID.isFlammable(block_id_xplus))
                		metadataToProperties.put("east", "true");
                	
                	if (BlockID.isFlammable(block_id_zplus))
                		metadataToProperties.put("south", "true");
                	
            	}
            	
            } else if (block_id == 55) {
            	// handle redstone wire TODO
            	
            	
            } else if (block_id == 63) {
            	// sign support is non-existent at this point
            	
//            	for (int i = 0; i < tileentities.size(); i++) {
//            		
//            		Tag<?> tag = tileentities.get(i);
//            		
//            		if (!(tag instanceof CompoundTag))
//            			continue;
//            		
//            		CompoundTag tileentity = (CompoundTag) tag;
//            		
//            		int tx = tileentity.getInt("x");
//            		int ty = tileentity.getInt("y");
//            		int tz = tileentity.getInt("z");
//            		
//            		if (tx != ox || ty != y || tz != oz)
//            			continue;
//            	
//            		if (!"Sign".equals(tileentity.getString("id")))
//        				break;
//            		
//            		String line1 = ((StringTag)tileentity.get("Text1")).getValue();
//            		String line2 = ((StringTag)tileentity.get("Text2")).getValue();
//            		String line3 = ((StringTag)tileentity.get("Text3")).getValue();
//            		String line4 = ((StringTag)tileentity.get("Text4")).getValue();
//            	}
            }

            return BlockState.of(bid.getModernId(), metadataToProperties);
        }

        public LightData getLightData(int x, int y, int z, LightData target) {
            if (blockLight.data.length == 0 && skyLight.data.length == 0) return target.set(0, 0);

            x &= 0xF; z &= 0xF;
            
            int blocklight = this.blockLight.data.length > 0 ? blockLight.getData(x, y, z) : 0;
            
            int block_id = this.blocks[x << 11 | z << 7 | y] & 255;
            
            // if slab or stairs, use max light value from neighboring blocks (except facing down)
            if (block_id == 44 || block_id == 53 || block_id == 67) {

                ChunkMcRegion chunkxminus = ((ChunkMcRegion) this.world.getChunkAtBlock(x-1, y, z));
                ChunkMcRegion chunkxplus = ((ChunkMcRegion) this.world.getChunkAtBlock(x+1, y, z));
                ChunkMcRegion chunkzminus = ((ChunkMcRegion) this.world.getChunkAtBlock(x, y, z-1));
                ChunkMcRegion chunkzplus = ((ChunkMcRegion) this.world.getChunkAtBlock(x, y, z+1));
                ChunkMcRegion chunkyplus = ((ChunkMcRegion) this.world.getChunkAtBlock(x, y+1, z));
                int lightxminus = chunkxminus.section.blockLight.data.length > 0 ? chunkxminus.section.blockLight.getData(x-1, y, z) : 0;
                int lightxplus = chunkxplus.section.blockLight.data.length > 0 ? chunkxplus.section.blockLight.getData(x+1, y, z) : 0;
                int lightzminus = chunkzminus.section.blockLight.data.length > 0 ? chunkzminus.section.blockLight.getData(x, y, z-1) : 0;
                int lightzplus = chunkzplus.section.blockLight.data.length > 0 ? chunkzplus.section.blockLight.getData(x, y, z+1) : 0;
                int lightyplus = chunkyplus.section.blockLight.data.length > 0 ? chunkyplus.section.blockLight.getData(x, y+1, z) : 0;

                blocklight = NumberUtils.max(lightxminus, lightxplus, lightzminus, lightzplus, lightyplus);
            }

            return target.set(
                    this.skyLight.data.length > 0 ? skyLight.getData(x, y, z) : 0,
                    blocklight
            );
        }
    }

}
