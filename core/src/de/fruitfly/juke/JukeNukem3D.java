package de.fruitfly.juke;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.TimeUtils;
//C:\Program Files (x86)\3DRealms\Anthology\Duke Nukem 3D
// http://gamedev.stackexchange.com/questions/43294/creating-a-retro-style-palette-swapping-effect-in-opengl
public class JukeNukem3D extends ApplicationAdapter {
	
	private static final String GAME_DIR = "../../gamedata";
	
	private MapFile map;
	
	private ImmediateModeRenderer20 sr;
	private ShapeRenderer shapes;
	private ImmediateModeRenderer20 colors;
	private PerspectiveCamera viewCam;
	private Player player;
	
	private FPSLogger fps = new FPSLogger();
	
	private Map<Integer, Texture> textures;
	private Wall selectedWall, selectedWallNext;
	
	@Override
	public void create () {
		GrpFile grpFile = new GrpFile(Gdx.files.absolute(GAME_DIR + "/DUKE3D.GRP"));
	
		//ArtFile art = new ArtFile(gameAssets, "TILES000.ART");
		//art.export("C:/tmp/duke/grpp/out3", GAME_DIR + "/PALETTE.DAT");
		
		String mapfileName = "E1L1.MAP";
		System.out.println("Loading " + mapfileName + "...");
		map = new MapFile();
		map.load(grpFile.getFile(mapfileName));
		//map.load(new FileInputStream(new File("C:/Users/daniel.platz/AppData/Local/VirtualStore/Program Files (x86)/3DRealms/Anthology/Duke Nukem 3D/newboard.map")));
		System.out.println("Done.");

		System.out.println("Tesselating sector planes... ");
		SectorTesselator tesselator = new SectorTesselator();
		for (Sector s : map.sectors) {
			tesselator.tesselateSector(map, s);
		}
		tesselator.dispose();
		System.out.println("Done.");
		
		System.out.println("Loading textures...");
		
		Set<Integer> picNumsToLoad = new HashSet<Integer>();
		for (Wall w : map.walls) {
			picNumsToLoad.add(w.picnum);
		}
		System.out.println("Number of textures: " + picNumsToLoad.size());
		
		ArtFile art = new ArtFile(grpFile, "TILES000.ART");
		textures = art.load(GAME_DIR + "/PALETTE.DAT", picNumsToLoad);
		System.out.println("Done.");
		
		viewCam = new PerspectiveCamera(75.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		viewCam.near = 0.1f;
		viewCam.far = 100000.0f;
		
		player = new Player(map.spawnX, map.spawnY, map.spawnZ);
		player.setYaw(map.spawnAngle);
		sr = new ImmediateModeRenderer20(false, true, 1);
		colors = new ImmediateModeRenderer20(false, true, 0);

		shapes = new ShapeRenderer();
		
//		VocSound sound = new VocSound((OpenALAudio)Gdx.audio, grpFile.getFile("DOOMED16.VOC"));
//		sound.play(0.2f);
	}

	long lastSecond = 0;
	
	private void processInputs() {
		if (Gdx.input.isButtonPressed(0) && !Gdx.input.isCursorCatched()) {
			Gdx.input.setCursorCatched(true);
			return;
		}
		
		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) && Gdx.input.isCursorCatched()) {
			Gdx.input.setCursorCatched(false);
			return;
		}
		
		float dx = Gdx.input.getDeltaX()/(float)Gdx.graphics.getWidth();
		float dz = Gdx.input.getDeltaY()/(float)Gdx.graphics.getHeight();
		
		player.setPitch(player.getPitch() - dz);
		player.setYaw(player.getYaw() - dx);

		Vector3 dir = new Vector3(MathUtils.cos(player.getYaw()), MathUtils.sin(player.getYaw()), MathUtils.sin(player.getPitch()));
		Vector3 side = new Vector3(dir).crs(0.0f, 0.0f, 1.0f).nor();
		
		float speed = 100.0f;
		
		if (Gdx.input.isKeyPressed(Keys.W)) {
			player.getPosition().add(dir.x* speed, dir.y* speed, dir.z* speed);
		}
		else if (Gdx.input.isKeyPressed(Keys.S)) {
			player.getPosition().add(dir.x*-speed, dir.y*-speed, dir.z*-speed);
		}
		
		if (Gdx.input.isKeyPressed(Keys.D)) {
			player.getPosition().add(side.x* speed, side.y* speed, side.z* speed);
		}
		else if (Gdx.input.isKeyPressed(Keys.A)) {
			player.getPosition().add(side.x*-speed, side.y*-speed, side.z*-speed);
		}

		if (Gdx.input.isButtonPressed(0)) {
			//Sector currentSector = findSector(map, player.getPosition().x, player.getPosition().y, player.getPosition().z);
			Wall minDistanceWall = null;
			Wall minDistanceWallNext = null;
			float minDistance = Float.POSITIVE_INFINITY;
			for (Sector s : map.sectors) {
				for (int wi=s.wallptr; wi<s.wallptr+s.wallnum; wi++) {
					Wall w1 = map.walls.get(wi);
					Wall w2 = map.walls.get(w1.point2);
					Vector3 normal = new Vector3(w2.y-w1.y, w2.x-w1.x, 0.0f);
					Plane plane = new Plane(normal, new Vector3(w1.x, w1.y, 0.0f));
					Ray ray = new Ray(player.getPosition(), dir);
					Vector3 intersection = new Vector3();
					if (Intersector.intersectRayPlane(ray, plane, intersection)) {
						float distance = intersection.dst(player.getPosition());
						if (distance < minDistance) {
							minDistanceWall = w1;
							minDistanceWallNext = w2;
							minDistance = distance;
						}
					}
				}
			}
			selectedWall = minDistanceWall;
			selectedWallNext = minDistanceWallNext;
			System.out.println("Picked wall @ distance " + minDistance + ": " + minDistanceWall);
		}
	}
	
	@Override
	public void render() {
		
		if (TimeUtils.millis()-lastSecond > 0) {
			fps.log();
			lastSecond = TimeUtils.millis();
		}
		
		processInputs();
		
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
				
		Gdx.gl.glClearColor(0.5f, 0.0f, 0.5f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		Gdx.gl.glEnable(GL20.GL_CULL_FACE);
		//Gdx.gl.glDisable(GL20.GL_CULL_FACE);

		Gdx.gl.glFrontFace(GL20.GL_CW);
		Gdx.gl.glCullFace(GL20.GL_BACK);
		//Gdx.gl.glEnable(GL20.GL_TEXTURE_2D);
		viewCam.position.set(player.getPosition());

		Vector3 dir = new Vector3(MathUtils.cos(player.getYaw()), MathUtils.sin(player.getYaw()), MathUtils.sin(player.getPitch()));
		
		viewCam.up.set(0.0f, 0.0f, 1.0f);
		viewCam.direction.set(dir);
		viewCam.update();
		
		/*
		R.shapes.setProjectionMatrix(viewCam.projection);
		R.shapes.setTransformMatrix(viewCam.view);
		
		R.shapes.begin(ShapeType.Filled);
		R.shapes.setColor(Color.RED);
		R.shapes.rect(0.0f, 0.0f, 10.0f, 10.0f);
		R.shapes.end();
		*/
		
		//T.tex.bind(0);
		
		for (Sector sector : map.sectors) {
			renderSector(sector);
		}
		
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

		
		if (selectedWall != null) {
			colors.begin(viewCam.combined, GL20.GL_LINES);
			colors.color(Color.GREEN);
			colors.vertex(selectedWall.x, selectedWall.y, player.getPosition().z);
			colors.color(Color.GREEN);
			colors.vertex(selectedWallNext.x, selectedWallNext.y, player.getPosition().z);
			colors.end();
		}
		
		render2D();
	}

	private void render2D() {
		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();
		shapes.setAutoShapeType(true);
		shapes.setProjectionMatrix(new Matrix4().setToOrtho2D(0.0f, 0.0f, width, height));
		shapes.setTransformMatrix(new Matrix4().idt());
		shapes.begin();
		shapes.setColor(0.0f, 1.0f, 0.0f, 1.0f);
		shapes.line(width/2.0f-4.5f, height/2.0f, width/2.0f+4.5f, height/2.0f);
		shapes.line(width/2.0f, height/2.0f-4.5f, width/2.0f, height/2.0f+4.5f);

		shapes.end();
	}

	public void renderSector(Sector sector) {
		/*
		// floor
		sr.begin(viewCam.combined, GL20.GL_TRIANGLES, T.texMap.get("floor"));
		renderFloorPlane(sector);
		sr.end();
		
		// ceil
		sr.begin(viewCam.combined, GL20.GL_TRIANGLES, T.texMap.get("ceil"));
		int numTris = sector.fbo.length/9;
		for (int i=0; i<numTris; i++) {
			for (int j=2; j>=0; j--) {
				float x = sector.fbo[i*9+j*3+0];
				float y = sector.fbo[i*9+j*3+1];
				float z = sector.fbo[i*9+j*3+2];
				sr.texCoord(x/16.0f, y/16.0f);
				sr.vertex(x, y, z + sector.ceilHeight);
			}
		}
		sr.end();
*/
		
		for (int i=0; i<sector.wallnum; i++) {
			int wi = sector.wallptr + i;
			Wall w1 = map.walls.get(wi);
			int x1 = (int)w1.x;
			int y1 = (int)w1.y;
			
			Wall w2 = map.walls.get(w1.point2);
			int x2 = (int) w2.x;
			int y2 = (int) w2.y;
			
			sr.begin(viewCam.combined, GL20.GL_TRIANGLES);


			if (w1.nextwall == 65535) {
				//renderColoredWall(x1, y1, sector.floorz, x2, y2, sector.ceilingz, new Color((w1.picnum%100)/100.0f, 0.4f, 1.0f-(w1.picnum%100)/100.0f, 1.0f));
			}
			else {
				Gdx.gl.glEnable(GL20.GL_TEXTURE_2D);
				Texture wallTexture = textures.get(w1.picnum);
				wallTexture.bind();
				Wall portal = map.walls.get(w1.nextwall);
				Sector portalSector = map.sectors.get(w1.nextsector);
				
				if (portalSector.floorz > sector.floorz) {
					renderWall(w1, x1, y1, sector.floorz, x2, y2, portalSector.floorz, wallTexture);
				}
				if (portalSector.ceilingz < sector.ceilingz) {
					renderWall(w1, x1, y1, portalSector.ceilingz, x2, y2, sector.ceilingz, wallTexture);
				}
			}
			
			Gdx.gl.glDisable(GL20.GL_TEXTURE_2D);

			//renderFloorPlane(sector, new Color((sector.floorpicnum%100)/100.0f, 0.4f, 1.0f-(sector.floorpicnum%100)/100.0f, 1.0f));
			//renderCeilingPlane(sector, new Color((sector.ceilingpicnum%100)/100.0f, 0.4f, 1.0f-(sector.ceilingpicnum%100)/100.0f, 1.0f));

			sr.end();

		}
		
	}
	
	private void renderWall(Wall wall, float x1, float y1, float z1, float x2, float y2, float z2, Texture tex) {
		float scale = (16.0f*tex.getWidth());
		float u1 = 0.0f;
		float v1 = 0.0f;
		float u2 = (float) Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)) / scale;
		float v2 = (z2-z1) / (16.0f*tex.getWidth());
		sr.color(Color.WHITE);
		sr.texCoord(u2, v1);
		sr.vertex(x2, y2, z1);
		sr.color(Color.WHITE);
		sr.texCoord(u1, v1);
		sr.vertex(x1, y1, z1);
		sr.color(Color.WHITE);
		sr.texCoord(u2, v2);
		sr.vertex(x2, y2, z2);
		
		sr.color(Color.WHITE);
		sr.texCoord(u2, v2);
		sr.vertex(x2, y2, z2);
		sr.color(Color.WHITE);
		sr.texCoord(u1, v1);
		sr.vertex(x1, y1, z1);
		sr.color(Color.WHITE);
		sr.texCoord(u1, v2);
		sr.vertex(x1, y1, z2);
	}
	
	private void renderColoredWall(float x1, float y1, float z1, float x2, float y2, float z2, Color c) {
		sr.color(c);
		sr.vertex(x2, y2, z1);
		sr.color(c);
		sr.vertex(x1, y1, z1);
		sr.color(c);
		sr.vertex(x2, y2, z2);
		
		sr.color(c);
		sr.vertex(x2, y2, z2);
		sr.color(c);
		sr.vertex(x1, y1, z1);
		sr.color(c);
		sr.vertex(x1, y1, z2);
	}
	
	
	private void renderFloorPlane(Sector sector, Color c) {
		int numTris = sector.fbo.length/9;
		for (int i=0; i<numTris; i++) {
			for (int j=0; j<3; j++) {
				float x = sector.fbo[i*9+j*3+0];
				float y = sector.fbo[i*9+j*3+1];
				float z = sector.fbo[i*9+j*3+2];
				sr.color(c);
				sr.vertex(x, y, z + sector.floorz);
			}
		}
	}
	
	private void renderCeilingPlane(Sector sector, Color c) {
		int numTris = sector.fbo.length/9;
		for (int i=0; i<numTris; i++) {
			for (int j=2; j>=0; j--) {
				float x = sector.fbo[i*9+j*3+0];
				float y = sector.fbo[i*9+j*3+1];
				float z = sector.fbo[i*9+j*3+2];
				sr.color(c);
				sr.vertex(x, y, z + sector.ceilingz);
			}
		}
	}
	
	// utils
	
	public static Sector findSector(MapFile map, float x, float y, float z) {
		for (Sector s : map.sectors) {
			
		}
		return null;
	}
}
