package de.fruitfly.juke;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class GrpFile {
	Map<String, Integer> directory = new HashMap<String, Integer>();
	FileHandle f;
	public GrpFile(FileHandle f) {
		this.f = f;
		DataInputStream in = new DataInputStream(f.read());
		try {
			byte[] markerChars = new byte[12];
			for (int i=0; i<12; i++) {
				markerChars[i] = in.readByte();
			}
			String markerString = new String(markerChars);
			
			if (!markerString.equals("KenSilverman")) {
				throw new RuntimeException("Invalid .grp file format.");
			}
			
			int numFiles = in.readUnsignedByte() | in.readUnsignedByte() << 8 | in.readUnsignedByte() << 16 | in.readUnsignedByte() << 24;

			int offset = 12 + 4 + 16*numFiles;
			for (int i=0; i<numFiles; i++) {
				byte[] filenameChars = new byte[12];
				for (int j=0; j<12; j++) {
					filenameChars[j] = in.readByte();
				}
				String filename = new String(filenameChars).trim();
				int size = in.readUnsignedByte() | in.readUnsignedByte() << 8 | in.readUnsignedByte() << 16 | in.readUnsignedByte() << 24;
				directory.put(filename, offset);
				//System.out.println(filename + "@offset " + offset + " ");
				offset += size;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public InputStream getFile(String filename) {
		DataInputStream in = new DataInputStream(f.read());
		
		if (!directory.keySet().contains(filename)) return null;
		int offset = directory.get(filename);
		try {
			in.skipBytes(offset);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			/*
			try {
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			*/
		}
		return in;
	}
}
