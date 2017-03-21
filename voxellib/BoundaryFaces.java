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
 *  @author Pablo Miranda Carranza
 */

package voxellib;

import java.util.function.*; //for the lambdas

/**
 * a Class containing the algorithms for generating quad meshes from
 * boolean voxel data.
 *	@author Pablo Miranda Carranza
 */


public class BoundaryFaces{

	private Consumer< float[]>  vertexEmiter;

	/**
	 * The constructor takes a functional interface of type Consumer{@literal <}float[]{@literal >}.
	 * so a lambda expression can be passed.
	 * @param 	vertexEmiter	possibly a lambda expresion for processing the vertices 
	*/

	public BoundaryFaces (Consumer<float[]>  vertexEmiter) {
		this.vertexEmiter=vertexEmiter;
	}

	private void addQuad(float[][] vertices, boolean isClockwise){
		if(isClockwise){
			for(int i=0;i<4;++i){
				vertexEmiter.accept(vertices[i].clone()); 
			}
		}
		else{
			for(int i=3;i>=0;--i){
				vertexEmiter.accept(vertices[i].clone()); 
			}
		}
	}

	/**
	 * Generates a mesh made out of quads from voxel data in the form of a
	 * three dimensional array of booleans. If the boolean value in the voxel
	 * is true, it will generate a box in tis place, otherwise it will leave it empty.
	 * The generated mesh consists only of the quad faces between true and false voxels,
	 * returned as sequences of vertices reported through the Consumer{@literal <}float[]{@literal >}
	 * vertexEmiter field and with consisting winding that defines the interior of the mesh as the voxels
	 * with a true value.
	 * @param 	voxels 		a 3 dimensional array of boolean values
	 * @param 	boxSize		the size of each cell (same width,length and height)	 
	*/

	public void makeQuadsFromVoxels(boolean[][][] voxels, float boxSize){

		float[][] vertices =new float[4][];


		for (int i = 0; i < voxels.length; ++i) {
			for (int j = 0; j < voxels[0].length; ++j) {
				for (int k = 0; k < voxels[0][0].length; ++k) {

		 			//interna walls perpendicular to x (i)
		 			if(i>0 && voxels[i-1][j][k] != voxels[i][j][k]){	
		 				vertices[0]=new float[]{i*boxSize,     j*boxSize,     k*boxSize};
		 				vertices[1]=new float[]{i*boxSize, (j+1)*boxSize,     k*boxSize};
		 				vertices[2]=new float[]{i*boxSize, (j+1)*boxSize, (k+1)*boxSize};
		 				vertices[3]=new float[]{i*boxSize,     j*boxSize, (k+1)*boxSize};
						//the winding depends of which one is true and which is false
						addQuad(vertices,voxels[i-1][j][k]);	
					}
					//internal walls perpendicular to y (j)
					if(j>0 && voxels[i][j-1][k] != voxels[i][j][k]){
						vertices[0]=new float[]{    i*boxSize, j*boxSize,     k*boxSize};
						vertices[1]=new float[]{(i+1)*boxSize, j*boxSize,     k*boxSize};
						vertices[2]=new float[]{(i+1)*boxSize, j*boxSize, (k+1)*boxSize};
						vertices[3]=new float[]{    i*boxSize, j*boxSize, (k+1)*boxSize};
						//the winding depends of which one is true and which is false
						addQuad(vertices,voxels[i][j][k]);
					}
					//internal walls perpendicular to z (k)
					if(k>0 && voxels[i][j][k-1] != voxels[i][j][k]){
						vertices[0]=new float[]{    i*boxSize,     j*boxSize, k*boxSize};
						vertices[1]=new float[]{(i+1)*boxSize,     j*boxSize, k*boxSize};
						vertices[2]=new float[]{(i+1)*boxSize, (j+1)*boxSize, k*boxSize};
						vertices[3]=new float[]{    i*boxSize, (j+1)*boxSize, k*boxSize};
						//the winding depends of which one is true and which is false
						addQuad(vertices,voxels[i][j][k-1]);
					}
				}
			}
		}

		//left and right "walls"
		for (int j = 0; j < voxels[0].length; ++j) {
			for (int k = 0; k < voxels[0][0].length; ++k) {

				//left
				if(voxels[0][j][k]){
					float xPos=0;
					vertices[0]=new float[]{xPos,     j*boxSize,     k*boxSize};
					vertices[1]=new float[]{xPos, (j+1)*boxSize,     k*boxSize};
					vertices[2]=new float[]{xPos, (j+1)*boxSize, (k+1)*boxSize};
					vertices[3]=new float[]{xPos,     j*boxSize, (k+1)*boxSize};
					addQuad(vertices,false);

				}

				//right
				if(voxels[voxels.length-1][j][k]){
					float xPos=voxels.length*boxSize;
					vertices[0]=new float[]{xPos,     j*boxSize,     k*boxSize};
					vertices[1]=new float[]{xPos, (j+1)*boxSize,     k*boxSize};
					vertices[2]=new float[]{xPos, (j+1)*boxSize, (k+1)*boxSize};
					vertices[3]=new float[]{xPos,     j*boxSize, (k+1)*boxSize};
					addQuad(vertices,true);
				}
			}
		}

		//front and back "walls"
		for (int i = 0; i < voxels.length; ++i) {
			for (int k = 0; k < voxels[0][0].length; ++k) {

				//front
				if(voxels[i][0][k]){
					float yPos=0;
					vertices[0]=new float[]{    i*boxSize, yPos,     k*boxSize};
					vertices[1]=new float[]{(i+1)*boxSize, yPos,     k*boxSize};
					vertices[2]=new float[]{(i+1)*boxSize, yPos, (k+1)*boxSize};
					vertices[3]=new float[]{    i*boxSize, yPos, (k+1)*boxSize};
					addQuad(vertices,true);
				}

				//back
				if(voxels[i][voxels[0].length-1][k]){
					float yPos=voxels[0].length*boxSize;
					vertices[0]=new float[]{    i*boxSize, yPos,     k*boxSize};
					vertices[1]=new float[]{(i+1)*boxSize, yPos,     k*boxSize};
					vertices[2]=new float[]{(i+1)*boxSize, yPos, (k+1)*boxSize};
					vertices[3]=new float[]{    i*boxSize, yPos, (k+1)*boxSize};
					addQuad(vertices,false);
				}
			}
		}


		//bottom and top 
		for (int i = 0; i < voxels.length; ++i) {
			for (int j = 0; j < voxels[0].length; ++j) {

				//bottom	
				if(voxels[i][j][0]){
					float zPos=0;
					vertices[0]=new float[]{    i*boxSize,     j*boxSize, zPos};
					vertices[1]=new float[]{(i+1)*boxSize,     j*boxSize, zPos};
					vertices[2]=new float[]{(i+1)*boxSize, (j+1)*boxSize, zPos};
					vertices[3]=new float[]{    i*boxSize, (j+1)*boxSize, zPos};
					addQuad(vertices,false);
				}	

				//Top
				if(voxels[i][j][voxels[0][0].length-1]){
					float zPos=voxels[0][0].length*boxSize;
					vertices[0]=new float[]{    i*boxSize,     j*boxSize, zPos};
					vertices[1]=new float[]{(i+1)*boxSize,     j*boxSize, zPos};
					vertices[2]=new float[]{(i+1)*boxSize, (j+1)*boxSize, zPos};
					vertices[3]=new float[]{    i*boxSize, (j+1)*boxSize, zPos};
					addQuad(vertices,true);
				}	
			}
		}
	}

}