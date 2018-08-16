package org.jurassicraft.server.util;

public class Vec2d {
	
	public double x;
    public double y;

    public Vec2d(double xIn, double yIn)
    {
        this.x = xIn;
        this.y = yIn;
    }

	public static double distance(int x1, int z1, float x2, float z2) {
		return Math.sqrt((z1 - z2) * (z1 - z2) + (x1 - x2) * (x1 - x2));
	}

}