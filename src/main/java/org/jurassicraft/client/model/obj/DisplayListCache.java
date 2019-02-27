package org.jurassicraft.client.model.obj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.lwjgl.opengl.GL11;

public class DisplayListCache {
	
	private static HashMap<String, Long> listsTimes = new HashMap();
	public static HashMap<String, Integer> lists = new HashMap<>();
	private static ArrayList<String> queueRemove = new ArrayList<String>();
	private static long lastTime = CTM() / 1000L;
	private static int nanoSeconds = 3 * 1000000;
	
	/**
     * Purge an inactive DisplayList from the graphial RAM
     * @param value ID of the DisplayList
     */
	public static void removeDL(int value) {
		GL11.glDeleteLists(value, 1);
	}
	
	private static long CTM() {
		return System.currentTimeMillis();
	}
	
	/**
     * Check if the queue is able to receive new tasks
     */
	public static boolean isAvailable() {
		return nanoSeconds > 0;
	}
	
	/**
     * Creates new asynchronously executed thread for the renderer
     * @param renderer Java Runnable to define the execution tasks
     */
	public static int newRenderer(Runnable renderer) {
		final int displayList = GL11.glGenLists(1);
		GL11.glNewList(displayList, GL11.GL_COMPILE);
		final long start = System.nanoTime();
		renderer.run();
		final long end = System.nanoTime();
		GL11.glEndList();
		nanoSeconds -= end - start;
		return displayList;
	}

	public static void update() {
		nanoSeconds = 3 * 1000000; 
	}
	
	/**
     * Remove outdated lists
     * @param key Identifier of the list
     */
	public static synchronized void remove(String key) {
		queueRemove.add(key);
		
	}
	
	/**
     * Get a specific DisplayList by it's identifier
     * @param key Identifier of the list
     */
	public static synchronized Integer get(String key) {
		for(String i : queueRemove) {
			removeDL(lists.get(i));
			lists.remove(i);
			listsTimes.remove(i);
		}
		queueRemove.clear();
		if ((long)(lastTime + 3) < (long)(CTM() / 1000L)) {
			Set<String> ks = new HashSet<String>();
			ks.addAll(lists.keySet());
			for (String dk : ks) {
				if (dk != key && (long)(listsTimes.get(dk) + 3) < (long)(CTM() / 1000L)) {
					removeDL(lists.get(dk));
					lists.remove(dk);
					listsTimes.remove(dk);
				}
			}
			lastTime = CTM() / 1000L;
		}

		if (lists.containsKey(key)) {
			listsTimes.put(key, CTM() / 1000L);
			return lists.get(key);
		}
		return null;
	}

	/**
     * Add a new DisplayList to the Cache
     * @param key Identifier of the new value
     * @param value DisplayList ID of the new value
     */
	public static synchronized void put(String key, int value) {
		listsTimes.put(key, CTM() / 1000);
		lists.put(key, value);
	}

	/**
     * Retrieve and update all values in the cache
     */
	public static synchronized Collection<Integer> values() {
		for(String i : queueRemove) {
			removeDL(lists.get(i));
			lists.remove(i);
			listsTimes.remove(i);
		}
		queueRemove.clear();
		if ((long)(lastTime + 3) < (long)(CTM() / 1000L)) {
			Set<String> ks = new HashSet<String>();
			ks.addAll(lists.keySet());
			for (String dk : ks) {
				if ((long)(listsTimes.get(dk) + 3) < (long)(CTM() / 1000L)) {
					removeDL(lists.get(dk));
					lists.remove(dk);
					listsTimes.remove(dk);
				}
			}
			lastTime = CTM() / 1000L;
		}

		return lists.values();
	}
}
