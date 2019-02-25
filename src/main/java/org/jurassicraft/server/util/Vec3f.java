package org.jurassicraft.server.util;

public class Vec3f {

	public static final Vec3f ZERO = new Vec3f(0.0F, 0.0F, 0.0F);
	public float x;
	public float y;
	public float z;

	public Vec3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Vec3f))
			return false;
		
		Vec3f vec = (Vec3f) obj;
		return vec.x == this.x && vec.y == this.y && vec.z == this.z;
	}

	@Override
	public int hashCode() {
		long j = Float.floatToIntBits(this.x);
        int i = (int)(j ^ j >>> 32);
        j = Float.floatToIntBits(this.y);
        i = 31 * i + (int)(j ^ j >>> 32);
        j = Float.floatToIntBits(this.z);
        i = 31 * i + (int)(j ^ j >>> 32);
        return i;
	}
	
	/**
     * Subtract two three-dimensional vectors
     * @param vec Vec3f to subtract from the current object
     */
	public Vec3f subtract(Vec3f vec)
    {
        return new Vec3f(this.x - vec.x, this.y - vec.y, this.z - vec.z);
    }
	
	/**
     * Update the vector coordinates
     * @param x X-Coordinate
     * @param y Y-Coordinate
     * @param z Z-Coordinate
     */
	public void setVec(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

	@Override
    public String toString()
    {
        return "(" + this.x + ", " + this.y + ", " + this.z + ")";
    }

}