package de.fruitfly.juke;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.MathUtils;

class Sector {
	@Override
	public String toString() {
		return "sectortype [wallptr=" + wallptr + ", wallnum=" + wallnum
				+ ", ceilingz=" + ceilingz + ", floorz=" + floorz
				+ ", ceilingstat=" + ceilingstat + ", floorstat=" + floorstat
				+ ", ceilingpicnum=" + ceilingpicnum + ", ceilingheinum="
				+ ceilingheinum + ", ceilingshade=" + ceilingshade
				+ ", ceilingpal=" + ceilingpal + ", ceilingxpanning="
				+ ceilingxpanning + ", ceilingypanning=" + ceilingypanning
				+ ", floorpicnum=" + floorpicnum + ", floorheinum="
				+ floorheinum + ", floorshade=" + floorshade + ", floorpal="
				+ floorpal + ", floorxpanning=" + floorxpanning
				+ ", floorypanning=" + floorypanning + ", visibility="
				+ visibility + ", filler=" + filler + ", lotag=" + lotag
				+ ", hitag=" + hitag + ", extra=" + extra + "]";
	}
	int wallptr, wallnum;
	long ceilingz, floorz;
	int ceilingstat, floorstat;
	int ceilingpicnum, ceilingheinum;
	char ceilingshade;
	char ceilingpal, ceilingxpanning, ceilingypanning;
	int floorpicnum, floorheinum;
	char floorshade;
	char floorpal, floorxpanning, floorypanning;
	char visibility, filler;
	int lotag, hitag, extra;
	
	// renderdata
	float[] fbo;
}

class Wall {
	long x, y;
	int point2, nextwall, nextsector, cstat;
	int picnum, overpicnum;
	char shade;
	byte pal, xrepeat, yrepeat, xpanning, ypanning;
	int lotag, hitag, extra;
	@Override
	public String toString() {
		return "walltype [x=" + x + ", y=" + y + ", point2=" + point2
				+ ", nextwall=" + nextwall + ", nextsector=" + nextsector
				+ ", cstat=" + cstat + ", picnum=" + picnum + ", overpicnum="
				+ overpicnum + ", shade=" + shade + ", pal=" + pal
				+ ", xrepeat=" + xrepeat + ", yrepeat=" + yrepeat
				+ ", xpanning=" + xpanning + ", ypanning=" + ypanning
				+ ", lotag=" + lotag + ", hitag=" + hitag + ", extra=" + extra
				+ "]";
	}
}

public class MapFile {
int bytes = 0;
	
	public List<Sector> sectors;
	public List<Wall> walls;

	public int spawnX, spawnY, spawnZ;
	public float spawnAngle;
	
	public void load(InputStream str) {
		sectors = new ArrayList<Sector>();
		walls = new ArrayList<Wall>();
		
		try {
			DataInputStream in = new DataInputStream(str);
			
			long mapversion = readInt(in);
			if ((mapversion & 0xff) != 0x7) {
				System.out.println("Unknown mapversion " + mapversion);
			}
			
			spawnX = (int)readInt(in); // posx
			spawnY = -(int)readInt(in); // posy
			spawnZ = -(int)(readInt(in)>>4); // posz
			int rawAngle = readShort(in); // angle
			spawnAngle = (1.0f-rawAngle/2047.0f)*MathUtils.PI2;
			
			readShort(in); // cursecnum
			
			int numectors = readShort(in);
			for (int i=0; i<numectors; i++) {
				Sector sec = new Sector();
				readSector(in, sec);
				//if (i == 306) System.out.println(sec);
				sectors.add(sec);
			}
			
			int numwalls = readShort(in);
			for (int i=0; i<numwalls; i++) {
				Wall wall = new Wall();
				readWall(in, wall);
				System.out.println(wall);

				walls.add(wall);
			}
			/*
			Sector tsec = sectors.get(306);
			for (int i=tsec.wallptr; i<tsec.wallptr+tsec.wallnum; i++) {
				System.out.println(walls.get(i));
			}
			*/
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
	
	private void readWall(DataInputStream in, Wall wall) throws IOException {
		wall.x = readInt(in);
		wall.y = -readInt(in);
		wall.point2 = readShort(in);
		wall.nextwall = readShort(in);
		wall.nextsector = readShort(in);
		wall.cstat = readShort(in);
		wall.picnum = readShort(in);
		wall.overpicnum = readShort(in);
		wall.shade = (char) readByte(in); // todo
		wall.pal = in.readByte();
		wall.xrepeat = in.readByte();
		wall.yrepeat = in.readByte();
		wall.xpanning = in.readByte();
		wall.ypanning = in.readByte();
		wall.lotag = readShort(in);
		wall.hitag = readShort(in);
		wall.extra = readShort(in);
	}

	private void readSector(DataInputStream in, Sector sec) throws IOException {
		sec.wallptr = readShort(in);
		sec.wallnum = readShort(in);
		sec.ceilingz = -readInt(in)>>4;
		sec.floorz = -readInt(in)>>4;
		sec.ceilingstat = readShort(in);
		sec.floorstat = readShort(in);
		sec.ceilingpicnum = readShort(in);
		sec.ceilingheinum = readShort(in);
		sec.ceilingshade = (char) readByte(in);// todo
		sec.ceilingpal = (char) readByte(in); // todo
		sec.ceilingxpanning = (char) readByte(in); // todo
		sec.ceilingypanning = (char) readByte(in); // todo
		sec.floorpicnum = readShort(in);
		sec.floorheinum = readShort(in);
		sec.floorshade = (char) readByte(in); // todo
		sec.floorpal = (char) readByte(in); // todo
		sec.floorxpanning = (char) readByte(in); // todo
		sec.floorypanning = (char) readByte(in); // todo
		sec.visibility = (char) readByte(in); // todo
		sec.filler = (char) readByte(in); // todo
		sec.lotag = readShort(in);
		sec.hitag = readShort(in);
		sec.extra = readShort(in);
	}

	private long readInt(DataInputStream in) throws IOException {
		bytes+=4;
		long val = (in.readUnsignedByte() | in.readUnsignedByte() << 8 | in.readUnsignedByte() << 16 | in.readUnsignedByte() << 24);
		//System.out.println(String.format("0x%8s", Integer.toHexString(val)));
		return val;
	}
	
	private int readShort(DataInputStream in) throws IOException {
		bytes+=2;
		return (in.readUnsignedByte() | in.readUnsignedByte() << 8);
	}
	
	private int readByte(DataInputStream in) throws IOException {
		bytes+=1;
		return (in.readUnsignedByte());
	}
}
