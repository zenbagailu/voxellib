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
 *  @since  2017-03-17
 */

package voxellib;
import java.util.function.*; //for the lambdas

/**
 * A class containing the algorithms for generating triangular meshes from
 * float voxel data. It emits the vertices of each face, with correct winding,
 * and closes also the boundaries of the cube defined by the voxel data.
 *	@author Pablo Miranda Carranza
 */
 
public class Isosurface{

	private Consumer< float[]>  vertexEmiter;

	/**
	 * The constructor takes a functional interface of type Consumer{@literal <}float[]{@literal >}.
	 * so a lambda expression can be passed and used for reporting the vertices generated.
	 * @param 	vertexEmiter	possibly a lambda expresion for processing the vertices 
	*/

	public Isosurface (Consumer< float[]>  vertexEmiter) {
		this.vertexEmiter=vertexEmiter;
	}

	/**
	 * Generates a mesh made out of triangular faces from voxel data consisting 
	 * of a three dimensional array of float values. It uses a marching cubes algorithm 
	 * for generating a surface defining the interface between values larger and smaller 
	 * than a given level value. See <a href="https://en.wikipedia.org/wiki/Marching_cubes"> 
	 * the Wikipedia article</a> for more information. The Boundaries of the voxel data are closed using an equivalent 
	 * <a href="https://en.wikipedia.org/wiki/Marching_squares">Marching squares</a> algorithm.
	 * @param 	voxels 		a 3 dimensional array of boolean values
	 * @param 	boxSize		the size of each cell (same width,length and height)
	 * @param 	level		the float value used to define the isosurface
	*/

	//Using marching cube. 
	public void makeFromVoxels(float[][][] voxels, float boxSize, float level){
		
		MarchingCube cube=new MarchingCube(level, vertex->{ //lambda expression
			vertex[0]*=boxSize;
			vertex[1]*=boxSize;
			vertex[2]*=boxSize;
			vertexEmiter.accept(vertex); //it has already been cloned in MarchingCube
			}); 

		for (int i = 0; i < voxels.length; ++i) {
			for (int j = 0; j < voxels[0].length; ++j) {
				for (int k = 0; k < voxels[0][0].length; ++k) {
					cube.calculate(voxels, i, j, k);
				}
			}
		}

		//System.out.println("num of faces: "+ faces.size()/3);

		//now close the sides of the cube defined by voxels...

		// to make the closure work...
		class Position{
			int x;
			int y;
			int z;
		}
		final Position pos=new Position();

		float[][] squareVals=new float[4][2];

		//bottom and top

		MarchingSquare squareBT=new MarchingSquare(level, vertex->{ //lambda expression
			float[] vertex3D=new float[3];
			vertex3D[0]=(pos.x+vertex[0])*boxSize;
			vertex3D[1]=(pos.y+vertex[1])*boxSize;
			vertex3D[2]=pos.z*boxSize;
			vertexEmiter.accept(vertex3D);
		});

		for (int i = 0; i < voxels.length-1; ++i) {
			for (int j = 0; j < voxels[0].length-1; ++j) {
				
				pos.x=i;
				pos.y=j;

				//bottom
				squareVals[0][0]=voxels[i  ][j  ][0];
				squareVals[0][1]=voxels[i  ][j+1][0];
				squareVals[1][0]=voxels[i+1][j  ][0];
				squareVals[1][1]=voxels[i+1][j+1][0];
				pos.z=0;
				squareBT.calculateFaces(squareVals,MarchingSquare.Winding.Clockwise);

				//top
				int top=voxels[0][0].length-1;
				squareVals[0][0]=voxels[i  ][j  ][top];
				squareVals[0][1]=voxels[i  ][j+1][top];
				squareVals[1][0]=voxels[i+1][j  ][top];
				squareVals[1][1]=voxels[i+1][j+1][top];
				pos.z=voxels[0][0].length-1;
				squareBT.calculateFaces(squareVals,MarchingSquare.Winding.CounterClockwise);
			}
		}


		//left and right
		MarchingSquare squareLR=new MarchingSquare(level, vertex->{ //lambda expression
			float[] vertex3D=new float[3];
			vertex3D[0]=pos.x*boxSize;
			vertex3D[1]=(pos.y+vertex[0])*boxSize;
			vertex3D[2]=(pos.z+vertex[1])*boxSize;
			vertexEmiter.accept(vertex3D);
		});

		for (int j = 0; j < voxels[0].length-1; ++j) {
			for (int k = 0; k < voxels[0][0].length-1; ++k) {
		
				pos.y=j;
				pos.z=k;

				//left
				squareVals[0][0]=voxels[0][j  ][k  ];
				squareVals[0][1]=voxels[0][j  ][k+1];
				squareVals[1][0]=voxels[0][j+1][k  ];
				squareVals[1][1]=voxels[0][j+1][k+1];
				pos.x=0;
				squareLR.calculateFaces(squareVals,MarchingSquare.Winding.Clockwise);

				//right
				int right=voxels.length-1;
				squareVals[0][0]=voxels[right][j  ][k  ];
				squareVals[0][1]=voxels[right][j  ][k+1];
				squareVals[1][0]=voxels[right][j+1][k  ];
				squareVals[1][1]=voxels[right][j+1][k+1];
				pos.x=voxels.length-1;
				squareLR.calculateFaces(squareVals,MarchingSquare.Winding.CounterClockwise);
			}
		}

		//front and back
		MarchingSquare squareFB=new MarchingSquare(level, vertex->{ //lambda expression
			float[] vertex3D=new float[3];
			vertex3D[0]=(pos.x+vertex[0])*boxSize;
			vertex3D[1]=pos.y*boxSize;
			vertex3D[2]=(pos.z+vertex[1])*boxSize;
			vertexEmiter.accept(vertex3D);
		});

		for (int i = 0; i < voxels.length-1; ++i) {
			for (int k = 0; k < voxels[0][0].length-1; ++k) {

				pos.x=i;
				pos.z=k;

				//left
				squareVals[0][0]=voxels[i  ][0][k  ];
				squareVals[0][1]=voxels[i  ][0][k+1];
				squareVals[1][0]=voxels[i+1][0][k  ];
				squareVals[1][1]=voxels[i+1][0][k+1];
				pos.y=0;
				squareFB.calculateFaces(squareVals,MarchingSquare.Winding.CounterClockwise);

				//right
				int back=voxels[0].length-1;
				squareVals[0][0]=voxels[i  ][back][k  ];
				squareVals[0][1]=voxels[i  ][back][k+1];
				squareVals[1][0]=voxels[i+1][back][k  ];
				squareVals[1][1]=voxels[i+1][back][k+1];
				pos.y=voxels[0].length-1;
				squareFB.calculateFaces(squareVals,MarchingSquare.Winding.Clockwise);
			}
		}
	}

}