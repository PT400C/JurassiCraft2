package org.jurassicraft.client.model.animation.entity;

public class AnimState {
	
	public float angleR;
	public FootStatus statusR;
	public float ticksR;
	public float angleL;
	public FootStatus statusL;
	public float ticksL;
	
	public AnimState(float angleR, FootStatus statusR, float ticksR, float angleL, FootStatus statusL, float ticksL) {
		
		this.angleR = angleR;
		this.statusR = statusR;
		this.ticksR = ticksR;
		this.angleL = angleL;
		this.statusL = statusL;
		this.ticksL = ticksL;
		
	}
	
	public float getAngle(boolean isLeft) {
		return isLeft ? angleL : angleR;
	}
	
	public FootStatus getStatus(boolean isLeft) {
		return isLeft ? statusL : statusR;
	}
	
	public float getTicks(boolean isLeft) {
		return isLeft ? ticksL : ticksR;
	}
	
	public void setTicks(boolean isLeft, float ticks) {
		if(isLeft) {
			this.ticksL = ticks;
			return;
		}
		this.ticksR = ticks;
		
	}
	
	public void setStatus(boolean isLeft, FootStatus status) {
		if(isLeft) {
			this.statusL = status;
			return;
		}
		this.statusR = status;
		
	}
	
	public void setAngle(boolean isLeft, float angle) {
		if(isLeft) {
			this.angleL = angle;
			return;
		}
		this.angleR = angle;
		
	}

}
