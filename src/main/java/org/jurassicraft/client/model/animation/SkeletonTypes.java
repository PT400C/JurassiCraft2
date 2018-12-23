package org.jurassicraft.client.model.animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jurassicraft.server.entity.GrowthStage;
import org.jurassicraft.server.entity.dinosaur.BrachiosaurusEntity;
import org.jurassicraft.server.entity.dinosaur.TyrannosaurusEntity;

import com.google.common.collect.Lists;

import net.ilexiconn.llibrary.LLibrary;
import net.ilexiconn.llibrary.server.animation.Animation;
import net.ilexiconn.llibrary.server.animation.IAnimatedEntity;
import net.ilexiconn.llibrary.server.network.AnimationMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

public enum SkeletonTypes {
	
	DEATH("death", "tyrannosaurus"),
	ATTACKING("attacking", "tyrannosaurus");

    private List<String> classes;
    
    private String name;
    
    public static final SkeletonTypes[] VALUES = SkeletonTypes.values();

    SkeletonTypes(String name, String... classes) {
    	
    	this.name = name;
        this.classes = Arrays.asList(classes);
        
    }
    
    public List<String> getClasses() {
    	
    	return this.classes;
    	
    }
    
    public String getName() {
    	
    	return this.name;
    	
    }
}