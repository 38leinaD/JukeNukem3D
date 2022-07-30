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

import javax.imageio.ImageIO;

public class ArtExtractor {
	private int[] palette;
	private int bytes = 0;
	private int malformed = 0;
	public void convert(String firstArtFile, String outFolder) {
		try {

			int fileIndex = 0;
			while (true) {
				String inArtFile = firstArtFile.replace("000", String.format("%03d", fileIndex));
				if (!new File(inArtFile).exists()) break;
				
				DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(inArtFile)));
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
					System.out.println(i);
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
			System.out.println("Done; " + malformed + " malformed artworks.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
	
	public static void main(String[] args) {
		ArtExtractor conv = new ArtExtractor();
		conv.loadPalette("C:/tmp/duke/PALETTE.DAT");
		conv.convert("C:/tmp/duke/grpp/TILES000.ART", "C:/tmp/duke/grpp/out2");
	}
}
