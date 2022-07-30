package de.fruitfly.juke;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;

public class ArtFile {
	private String firstArtFile;
	private GrpFile grpFile;
	
	private int[] palette;
	private int bytes = 0;
	private int malformed = 0;

	public ArtFile(GrpFile grpFile, String firstArtFile) {
		this.firstArtFile = firstArtFile;
		this.grpFile = grpFile;
	}
	
	public Map<Integer, Texture> load(String paletteFile, Set<Integer> picNumsToLoad) {
		loadPalette(paletteFile);
		InputStream artFileStream = null;
		Map<Integer, Texture> textures = new HashMap<Integer, Texture>();
		try {
			int fileIndex = 0;
			int picnum = -1;
			while (true) {
				String inArtFile = firstArtFile.replace("000", String.format("%03d", fileIndex));
				if (artFileStream != null) artFileStream.close();
				artFileStream = grpFile.getFile(inArtFile);
				if (artFileStream == null) break;
				
				DataInputStream in = new DataInputStream(artFileStream);
				long artversion = readInt(in);
				if (artversion != 1) throw new RuntimeException("Unkown version: " + artversion);
				long numtiles = readInt(in); // unused
				long localtilestart = readInt(in);
				long localtileend = readInt(in);
				
				int[] tilesizx = new int[(int) (localtileend-localtilestart+1)];
				for (int i=0; i<tilesizx.length; i++) {
					tilesizx[i] = readShort(in);
				}
				
				int[] tilesizy = new int[(int) (localtileend-localtilestart+1)];
				for (int i=0; i<tilesizx.length; i++) {
					tilesizy[i] = readShort(in);
				}
				
				int[] picanm = new int[(int) (localtileend-localtilestart+1)];
				for (int i=0; i<tilesizx.length; i++) {
					picanm[i] = (int)readInt(in);
				}
	
				
				for (int i=0; i<tilesizx.length; i++) {
					picnum++;

					if (tilesizx[i] == 0 || tilesizy[i] == 0) {
						malformed++;
						continue;
					}

					if (!picNumsToLoad.contains(picnum)) {
						in.skip(tilesizx[i] * tilesizy[i]);
						continue;
					}
					
					Pixmap pm = new Pixmap(tilesizx[i], tilesizy[i], Format.RGBA8888);
					for (int x=0; x<pm.getWidth(); x++) {
						for (int y=0; y<pm.getHeight(); y++) {
							int pi = readByte(in);
							pm.drawPixel(x, pm.getHeight()-y-1, (palette[pi] & 0x00ffffff) << 8 | 0xff);
						}
					}
					Texture t = new Texture(pm);
					t.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
					t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
					pm.dispose();
					textures.put(picnum, t);
				}
				
				fileIndex++;
			}
			//System.out.println("Done; " + malformed + " malformed artworks.");
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (artFileStream != null) {
				try {
					artFileStream.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		return textures;
	}
	
	public void export(String outFolder, String paletteFile) {
		loadPalette(paletteFile);
		
		InputStream artFileStream = null;
		try {
			int fileIndex = 0;
			while (true) {
				String inArtFile = firstArtFile.replace("000", String.format("%03d", fileIndex));
				if (artFileStream != null) artFileStream.close();
				artFileStream = grpFile.getFile(inArtFile);
				if (artFileStream == null) break;
				
				DataInputStream in = new DataInputStream(artFileStream);
				long artversion = readInt(in);
				if (artversion != 1) throw new RuntimeException("Unkown version: " + artversion);
				long numtiles = readInt(in); // unused
				long localtilestart = readInt(in);
				long localtileend = readInt(in);
				
				int[] tilesizx = new int[(int) (localtileend-localtilestart+1)];
				for (int i=0; i<tilesizx.length; i++) {
					tilesizx[i] = readShort(in);
				}
				
				int[] tilesizy = new int[(int) (localtileend-localtilestart+1)];
				for (int i=0; i<tilesizx.length; i++) {
					tilesizy[i] = readShort(in);
				}
				
				int[] picanm = new int[(int) (localtileend-localtilestart+1)];
				for (int i=0; i<tilesizx.length; i++) {
					picanm[i] = (int)readInt(in);
				}
	
				String filePrefix = new File(inArtFile).getName().replace(".ART", "");
				
				for (int i=0; i<tilesizx.length; i++) {
					if (tilesizx[i] == 0 || tilesizy[i] == 0) {
						malformed++;
						continue;
					}
					BufferedImage img = new BufferedImage(tilesizx[i], tilesizy[i], BufferedImage.TYPE_INT_ARGB);
					WritableRaster r = img.getRaster();
					DataBuffer b = r.getDataBuffer();
						for (int x=0; x<img.getWidth(); x++) {
							for (int y=0; y<img.getHeight(); y++) {
							int pi = readByte(in);
							b.setElem(y*img.getWidth()+x, palette[pi]);
						}
					}
	
					ImageIO.write(img, "PNG", new File(outFolder + "/" + filePrefix + "." + i + ".PNG"));
				}
				
				fileIndex++;
			}
			//System.out.println("Done; " + malformed + " malformed artworks.");
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (artFileStream != null) {
				try {
					artFileStream.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	private void loadPalette(String paletteFile) {
		palette = new int[256];
		
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(paletteFile)));
			for (int i=0; i<palette.length; i++) {
				int color = (0xff000000 | in.readUnsignedByte() << 16 | in.readUnsignedByte() << 8 | in.readUnsignedByte())*4;
				palette[i] = color;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
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
	
	/*
	public static void main(String[] args) {
		ArtFile conv = new ArtFile();
		conv.loadPalette("C:/tmp/duke/PALETTE.DAT");
		conv.convert("C:/tmp/duke/grpp/TILES000.ART", "C:/tmp/duke/grpp/out3");
	}*/
}
