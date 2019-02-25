package org.jurassicraft.server.util;

public class Vec2f {
	
	public final float x;
	public final float y;

	public Vec2f(float x, float y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Vec2f))
			return false;
		
		Vec2f v = (Vec2f) obj;
		return v.x == x && v.y == y;
	}

	@Override
	public int hashCode() {
		long j = Float.floatToIntBits(this.x);
        int i = (int)(j ^ j >>> 32);
        j = Float.floatToIntBits(this.y);
        i = 31 * i + (int)(j ^ j >>> 32);
        return i;
	}
	
	/**
     * Subtract two two-dimensional vectors
     * @param vec Vec2f to subtract from the current object
     */
	public Vec2f subtract(Vec2f vec)
    {
        return new Vec2f(this.x - vec.x, this.y - vec.y);
    }

	@Override
    public String toString()
    {
        return "(" + this.x + ", " + this.y + ")";
    }

}
