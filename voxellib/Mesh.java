/*
Copyright 2017 Pablo Miranda Carranza

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

/**
 *  voxellib package, library for processing voxels.
 *
 *	@author Pablo Miranda Carranza
 */

package voxellib;

import java.util.ArrayList;
import java.io.*;
import java.util.Arrays;
import java.lang.*;
import java.nio.channels.*;
import java.nio.*;

import processing.core.*; //for the QUADS definition and drawing


/**
 * a Class to generate meshes from voxel data, render and save them 
 * in .stl (sterolithography) binary format.
 *	@author Pablo Miranda Carranza
 */


public class Mesh {

	PApplet processing;
	ArrayList<float[]> faces;
	int type;

	/**
	 * Constructor.
	 * @param 	processing 	the instance of PApplet in which it should draw.
	 */

	public Mesh(PApplet processing){
		this.processing=processing;
		faces=new ArrayList<float[]>();
	}

	/**
	 * Generates a mesh made of quads from voxel data consisting of a
	 * three dimensional array of boolean values. If the value in the voxel
	 * is true, it will generate a box in its place, otherwise it will leave it empty.
	 * The generated mesh consists only of the faces between true and false voxels.
	 * @param 	voxels 		a 3 dimensional array of boolean values
	 * @param 	boxSize		the size of each cell (same width,length and height)	 
	*/

	public void makeFromVoxels(boolean[][][] voxels, float boxSize){
		type=PConstants.QUADS;
		faces.clear();
		BoundaryFaces bFaces=new BoundaryFaces(vertex->faces.add(vertex));//lambda expression
		bFaces.makeQuadsFromVoxels(voxels,boxSize);
	}

	/**
	 * Generates a mesh made out of triangular faces from from voxel data consisting 
	 * of a three dimensional array of float values. It uses a marching cubes algorithm 
	 * for generating a surface defining the interface between values larger and smaller 
	 * than a given level value. See <a href="https://en.wikipedia.org/wiki/Marching_cubes"> 
	 * the Wikipedia article</a> for more information. The Boundaries of the voxel data are closed using an equivalent 
	 * <a href="https://en.wikipedia.org/wiki/Marching_squares">Marching squares</a> algorithm.
	 * @param 	voxels 		a 3 dimensional array of boolean values
	 * @param 	boxSize		the size of each cell (same width,length and height)
	 * @param 	level		the float value used to define the isosurface
	*/

	public void makeFromVoxels(float[][][] voxels, float boxSize, float level){
		type=PConstants.TRIANGLES;
		faces.clear();
		Isosurface isoSurf=new Isosurface(vertex->faces.add(vertex));//lambda expression
		isoSurf.makeFromVoxels(voxels, boxSize, level);
	}


	/**
	 * It draws the mesh using PApplet.
	*/

	public void draw(){

		processing.beginShape(type);

		for(float[] v:faces){
			processing.vertex(v[0],v[1],v[2]);
		}

		processing.endShape();
	}

	/**
	 * This saves the mesh as a binary .stl (sterolithography) file, that can be used
	 * for 3D printing or otherwise to open in a modelling or CAD package. 
	 * @param 	fileName 	the path and name of the file we want to save. If only a filename is given
	 * the file will be saved in the local directory of the application.
	*/

	public void saveAsStl(String fileName){

		try(FileChannel ch=new RandomAccessFile(processing.sketchPath("")+fileName,"rw").getChannel())
		{

			writeStl(ch);

		}catch (FileNotFoundException except) {
			System.out.println(except.getMessage());
		}catch (IOException except){
			System.out.println(except.getMessage());
		}catch(BufferOverflowException except){
			System.out.println(except.getMessage());
		}

		System.out.println("Done writing stl file.");
	}


	/**
	* Stl definitions implemented directly from 
	*  <a href="https://en.wikipedia.org/wiki/STL_(file_format)">Wikipedia</a>
	* @param ch 	the FileChannel used for writing in stl format. 
	**/

	private void writeStl(FileChannel ch) throws IOException {

		int numOfTriangles=0;


		//100 bytes, the size of two faces, is the maximum size used for
		//the buffer
		ByteBuffer bb=ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);


		//UINT8[80] – Header
		byte[] header=new byte[80];
		// byte[] info="file produced by voxelib.".getBytes();
		// System.arraycopy( info, 0, header, 0, info.length );
		bb.put(header);
		bb.flip();
		ch.write(bb);
		bb.clear();


		//UINT32 – Number of triangles
		if(type==PConstants.QUADS){
			numOfTriangles =faces.size()/2; //the number of quads (vertices/4) * 2 
			//System.out.println("Num of triangles: " + numOfTriangles);
		}else if(type==PConstants.TRIANGLES){
			numOfTriangles =faces.size()/3;
		}
		
		//a java int is 32 bits, though it is signed. It should not make a difference 
		//if it is possitive (which will always be)
		bb.putInt(numOfTriangles);
		bb.flip();
		ch.write(bb);
		bb.clear();

		// foreach triangle
		// REAL32[3] – Normal vector
		// REAL32[3] – Vertex 1
		// REAL32[3] – Vertex 2
		// REAL32[3] – Vertex 3
		// UINT16 – Attribute byte count
		// end

		//a java float is a 32-bit IEEE 754, the same format used by stl 

		if(type==PConstants.QUADS){
			for(int i=0; i<faces.size(); i+=4){

				float[] v0=faces.get(i  );
				float[] v1=faces.get(i+1);
				float[] v2=faces.get(i+2);
				float[] v3=faces.get(i+3);

				float[] normal=calculateNormal(v0,v1,v2);
				//float[] normal={0,0,0}; //not really necessary to calculate
				//first triangle
				bb.putFloat(normal[0]); bb.putFloat(normal[1]); bb.putFloat(normal[2]);
				bb.putFloat(v0[0]); bb.putFloat(v0[1]); bb.putFloat(v0[2]);
				bb.putFloat(v1[0]); bb.putFloat(v1[1]); bb.putFloat(v1[2]);
				bb.putFloat(v2[0]); bb.putFloat(v2[1]); bb.putFloat(v2[2]);
				bb.putShort((short)0); //the UINT16

				//second triangle
				bb.putFloat(normal[0]); bb.putFloat(normal[1]); bb.putFloat(normal[2]);
				bb.putFloat(v0[0]); bb.putFloat(v0[1]); bb.putFloat(v0[2]);
				bb.putFloat(v2[0]); bb.putFloat(v2[1]); bb.putFloat(v2[2]);
				bb.putFloat(v3[0]); bb.putFloat(v3[1]); bb.putFloat(v3[2]);
				bb.putShort((short)0); //the UINT16

				bb.flip();
				ch.write(bb);
				bb.clear();
			}
		}
		else if(type==PConstants.TRIANGLES){
			for(int i=0; i<faces.size(); i+=3){
				float[] v0=faces.get(i  );
				float[] v1=faces.get(i+1);
				float[] v2=faces.get(i+2);

				float[] normal=calculateNormal(v0,v1,v2);

				bb.putFloat(normal[0]); bb.putFloat(normal[1]); bb.putFloat(normal[2]);
				bb.putFloat(v0[0]); bb.putFloat(v0[1]); bb.putFloat(v0[2]);
				bb.putFloat(v1[0]); bb.putFloat(v1[1]); bb.putFloat(v1[2]);
				bb.putFloat(v2[0]); bb.putFloat(v2[1]); bb.putFloat(v2[2]);
				bb.putShort((short)0); //the UINT16

				bb.flip();
				ch.write(bb);
				bb.clear();
			}

		}
	}

	/**
	* standard normal calculation, used in generating the stl file.
	* The direction of the normal will be depend on the order of the vertices
	* (the normal will will follow the right-hand rule convention).
	* @param v0 a vertex of the face
	* @param v1 second vertex of the face
	* @param v2 and a third vertex of the face
	* @return 	the normal as an array of 3 floats.        	
	**/

	private float[] calculateNormal(float[] v0, float[] v1, float[] v2){

		float[] v01={v1[0]-v0[0],v1[1]-v0[1],v1[2]-v0[2]};
		float[] v02={v2[0]-v0[0],v2[1]-v0[1],v2[2]-v0[2]};

		//cross product 
		float[] normal = new float[3];

		normal[0]=v01[1]*v02[2]-v01[2]*v02[1];
		normal[1]=v01[2]*v02[0]-v01[0]*v02[2];
		normal[1]=v01[0]*v02[1]-v01[1]*v02[0];

		//it needs to be normalised...
		float size=normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2];
		size=(float)Math.sqrt(size);
		normal[0]/=size;
		normal[1]/=size;
		normal[2]/=size;

		return normal;
	}
}