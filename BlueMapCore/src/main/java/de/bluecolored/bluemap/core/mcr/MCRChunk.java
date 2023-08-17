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

import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.LightData;
import net.querz.nbt.CompoundTag;

import java.io.IOException;

public abstract class MCRChunk implements Chunk {

    private final MCRWorld world;
    private final int dataVersion;

    protected MCRChunk() {
        this.world = null;
        this.dataVersion = -1;
    }

    protected MCRChunk(MCRWorld world) {
        this.world = world;
        this.dataVersion = -1;
    }

    protected MCRChunk(MCRWorld world, CompoundTag chunkTag) {
        this.world = world;
        dataVersion = chunkTag.getInt("DataVersion");
    }

    @Override
    public abstract boolean isGenerated();

    public int getDataVersion() {
        return dataVersion;
    }

    @Override
    public abstract long getInhabitedTime();

    @Override
    public abstract BlockState getBlockState(int x, int y, int z);

    @Override
    public abstract LightData getLightData(int x, int y, int z, LightData target);

    @Override
    public abstract String getBiome(int x, int y, int z);

    @Override
    public int getMaxY(int x, int z) {
        return 255;
    }

    @Override
    public int getMinY(int x, int z) {
        return 0;
    }

    @Override
    public int getWorldSurfaceY(int x, int z) { return 0; }

    @Override
    public int getOceanFloorY(int x, int z) { return 0; }

    protected MCRWorld getWorld() {
        return world;
    }

    public static MCRChunk create(MCRWorld world, CompoundTag chunkTag) throws IOException {
        return new ChunkMcRegion(world, chunkTag);
    }

    @Override
    public String toString() {
        return "MCRChunk{" +
               "world=" + world +
               "dataVersion=" + dataVersion +
               "isGenerated()=" + isGenerated() +
               '}';
    }

}
