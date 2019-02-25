package org.jurassicraft.client.event;

import org.jurassicraft.client.model.obj.OBJHandler;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;

public class ResourceReloadListener implements IResourceManagerReloadListener {
	
	private static boolean init = true;

	/**
	 * Reset all OBJ models
	 * @param resourceManager IResourceManager instance
	 */
	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
		
		if(init) {
			init = false;
			return;
		}
		OBJHandler.renderer.forEach((k, v) -> v.purgeTextures());
		OBJHandler.init(null);
		
	}

}
