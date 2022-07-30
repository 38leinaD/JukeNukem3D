package de.fruitfly.juke;

import java.util.Arrays;

import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellator;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;

import com.badlogic.gdx.graphics.GL20;

public class SectorTesselator extends GLUtessellatorCallbackAdapter  {

	private GLUtessellator tesselator;
	
	private int primitiveType;
	private float[] primitiveVertices = new float[1000];
	private int primtiveVertexCount;
	
	private float[] tesselationTris = new float[1000];
	private int tesselationTriVertexCount;
	
	public SectorTesselator() {
		// Create a new tessellation object
		tesselator = GLU.gluNewTess();
		// Set callback functions

		tesselator.gluTessCallback(GLU.GLU_TESS_VERTEX, this);
		tesselator.gluTessCallback(GLU.GLU_TESS_BEGIN, this);
		tesselator.gluTessCallback(GLU.GLU_TESS_END, this);
		tesselator.gluTessCallback(GLU.GLU_TESS_COMBINE, this);
		
		tesselator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_POSITIVE);
	}
/*
	void setWindingRule(int windingRule) {
		// Set the winding rule
		tesselator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, windingRule);
	}
*/
	void tesselateSector(MapFile map, Sector s) {
		tesselationTriVertexCount = 0; 
		tesselator.gluTessBeginPolygon(null);
		
		boolean contourStarted = false;
		for (int wi=s.wallptr; wi<s.wallptr+s.wallnum; wi++) {
			Wall w = map.walls.get(wi);
			if (!contourStarted) {
				tesselator.gluTessBeginContour();
				contourStarted = true;
			}
				double[] vertexData = new double[] { w.x, w.y, 0.0 }; 
				tesselator.gluTessVertex(vertexData, 0, vertexData);
			if (w.point2 < wi) {
				tesselator.gluTessEndContour();
				contourStarted = false;
			}
		}
		
		tesselator.gluTessEndPolygon();
		
		s.fbo = Arrays.copyOf(tesselationTris, tesselationTriVertexCount*3);
	}

	public void begin(int type) {
		// GL_TRIANGLE, GL_TRIANGLE_FAN, GL_TRIANGLE_STRIP
		primitiveType = type;
		primtiveVertexCount = 0;
		if (primitiveType == GL20.GL_TRIANGLES) {
		}
		else if (primitiveType == GL20.GL_TRIANGLE_FAN) {
		}
		else if (primitiveType == GL20.GL_TRIANGLE_STRIP) {
		}
		else {
			new RuntimeException("Unkown type " + primitiveType);
		}
	}
	
	public void vertex(Object vertexData) {
		double[] vd = (double[]) vertexData;
		
		float x = (float)vd[0];
		float y = (float)vd[1];
		float z = (float)vd[2];
		
		primitiveVertices[primtiveVertexCount*3 + 0] = x;
		primitiveVertices[primtiveVertexCount*3 + 1] = y;
		primitiveVertices[primtiveVertexCount*3 + 2] = z;
		
		primtiveVertexCount++;
	}
	
	public void end() {
		if (primitiveType == GL20.GL_TRIANGLES) {
			for (int i=0; i<primtiveVertexCount; i++) {
				tesselationTris[tesselationTriVertexCount*3 + 0] = primitiveVertices[i*3 + 0];
				tesselationTris[tesselationTriVertexCount*3 + 1] = primitiveVertices[i*3 + 1];
				tesselationTris[tesselationTriVertexCount*3 + 2] = primitiveVertices[i*3 + 2];
				tesselationTriVertexCount++;
			}
		}
		else if (primitiveType == GL20.GL_TRIANGLE_FAN) {
			for (int i=2; i<primtiveVertexCount; i++) {
				tesselationTris[tesselationTriVertexCount*3 + 0] = primitiveVertices[0];
				tesselationTris[tesselationTriVertexCount*3 + 1] = primitiveVertices[1];
				tesselationTris[tesselationTriVertexCount*3 + 2] = primitiveVertices[2];
				tesselationTriVertexCount++;
				
				tesselationTris[tesselationTriVertexCount*3 + 0] = primitiveVertices[(i-1)*3 + 0];
				tesselationTris[tesselationTriVertexCount*3 + 1] = primitiveVertices[(i-1)*3 + 1];
				tesselationTris[tesselationTriVertexCount*3 + 2] = primitiveVertices[(i-1)*3 + 2];
				tesselationTriVertexCount++;
				
				tesselationTris[tesselationTriVertexCount*3 + 0] = primitiveVertices[i*3 + 0];
				tesselationTris[tesselationTriVertexCount*3 + 1] = primitiveVertices[i*3 + 1];
				tesselationTris[tesselationTriVertexCount*3 + 2] = primitiveVertices[i*3 + 2];
				tesselationTriVertexCount++;
			}
		}
		else if (primitiveType == GL20.GL_TRIANGLE_STRIP) {
			for (int i=0; i<primtiveVertexCount-2; i++) {
				int a, b;
				if (i%2==0) {
					a = i;
					b = i+1;
				}
				else {
					a = i+1;
					b = i;
				}
				
				tesselationTris[tesselationTriVertexCount*3 + 0] = primitiveVertices[a*3 + 0];
				tesselationTris[tesselationTriVertexCount*3 + 1] = primitiveVertices[a*3 + 1];
				tesselationTris[tesselationTriVertexCount*3 + 2] = primitiveVertices[a*3 + 2];
				tesselationTriVertexCount++;
				
				tesselationTris[tesselationTriVertexCount*3 + 0] = primitiveVertices[b*3 + 0];
				tesselationTris[tesselationTriVertexCount*3 + 1] = primitiveVertices[b*3 + 1];
				tesselationTris[tesselationTriVertexCount*3 + 2] = primitiveVertices[b*3 + 2];
				tesselationTriVertexCount++;
				
				tesselationTris[tesselationTriVertexCount*3 + 0] = primitiveVertices[(i+2)*3 + 0];
				tesselationTris[tesselationTriVertexCount*3 + 1] = primitiveVertices[(i+2)*3 + 1];
				tesselationTris[tesselationTriVertexCount*3 + 2] = primitiveVertices[(i+2)*3 + 2];
				tesselationTriVertexCount++;
			}
		}
	}
	
	@Override
	public void error(int errnum) {
		throw new RuntimeException("tesselation error " + errnum);
	}

	@Override
	public void errorData(int errnum, Object polygonData) {
		throw new RuntimeException("tesselation error " + errnum + " with data " + polygonData);
	}

	public void dispose() {
		tesselator.gluDeleteTess();
	}
}