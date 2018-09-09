package org.jurassicraft.client.tablet;
import org.jurassicraft.server.util.DinosaurInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

public class RenderDinosaurInfo {
	
    public final DinosaurInfo info;
    public final int x;
    public final int z;
    public RenderDinosaurInfo(EntityPlayer player, DinosaurInfo info) {
    	
        this.info = info;
        this.x = BlockPos.fromLong(info.pos).getX();
        this.z = BlockPos.fromLong(info.pos).getZ();
    }
}