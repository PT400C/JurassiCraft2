package org.jurassicraft.client.tablet;

import org.jurassicraft.server.entity.DinosaurEntity;

public class DinosaurInfo {
		public final long pos;
		public final String type;
		public final boolean male;
		public final int existed;
		public final String name;

		DinosaurInfo(long pos, String type, boolean male, int existed, String name) {
			this.pos = pos;
			this.type = type;
			this.male = male;
			this.existed = existed;
			this.name = name;
		}

		public static DinosaurInfo fromEntity(DinosaurEntity entity) {
	
			return new DinosaurInfo(entity.getPosition().toLong(), entity.getDinosaur().getRegistryName().getResourcePath(), entity.isMale(), (byte) entity.getDaysExisted(), entity.hasCustomName() ? entity.getCustomNameTag() : "");

		}
		
		public static DinosaurInfo fromData(long pos, String type, boolean male, int existed, String name) {
			return new DinosaurInfo(pos, type, male, existed, name);
		}


		
	}