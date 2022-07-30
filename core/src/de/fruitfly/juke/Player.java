package de.fruitfly.juke;

import com.badlogic.gdx.math.Vector3;

public class Player {
	private Vector3 position = new Vector3();
	private float yaw, pitch;
	
	public Player(float x, float y, float z) {
		position.x = x;
		position.y = y;
		position.z = z;
	}
	
	public float getYaw() {
		return yaw;
	}
	public void setYaw(float yaw) {
		this.yaw = yaw;
	}
	public float getPitch() {
		return pitch;
	}
	public void setPitch(float pitch) {
		this.pitch = pitch;
	}
	public Vector3 getPosition() {
		return position;
	}
}