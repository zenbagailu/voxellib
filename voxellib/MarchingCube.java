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

import java.util.function.*;

/**
 * a Class implementing the classic marching cube algorithm with look-up tables.
 * The tables are defined on the separate LookUpTables class for convenience and readability.
 *  @author Pablo Miranda Carranza
 */


public class MarchingCube
{

  private float level;
  private Consumer< float[]>  vertexEmiter;
  //this is just a simple way of keeping trac off the ofsets of the positions
  private static final int[][] offsets={{0,0,0},{1,0,0},{1,1,0},{0,1,0},{0,0,1},{1,0,1},{1,1,1},{0,1,1}};
  //the list of edges. Each index in edges refers to an index in offsets (or indices) indicating a position
  //they represent the bottom edges, the top edges, and the sizes of the cube...
  private static final int[][] edges={{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
  private int[][] positions;
  private float[][] faceVertices;

  /**
    * The constructor takes a functional interface of type Consumer{@literal <}float[]{@literal >}.
   * so a lambda expression can be passed to it, to process the vertices.
   * @param   level         the level we want to use to define the boundary surface.
   * @param   vertexEmiter  possibly a lambda expresion for processing the vertices 
  **/

  public MarchingCube(float level, Consumer< float[]>  vertexEmiter){
    this.level=level;
    this.vertexEmiter=vertexEmiter;
    positions = new int[8][3];
    faceVertices = new float[12][3];
  }

  private float getForVertex(float[][][]vals,int index){
    int[] indx=positions[index];
    return vals[indx[0]][indx[1]][indx[2]];
  }


  /**
   *  Given a grid cell and a level, calculate the triangular facets required to represent the isosurface through the cell.
   *  It reports the vertices of the triangular faces through the vertexEmiter, at most there will be 5 triangular facets.
   *  @param voxels   a 3dimensional array of floats
   *  @param posX     the first index in the array where the cube is positioned
   *  @param posY     the second index
   *  @param posZ     the third index
  **/

  public void calculate(float[][][] voxels, int posX, int posY, int posZ){
     /*
        Determine the index into the edge table which
        tells us which vertices are inside of the surface
     */

    //fill in the positions table, for easier look up...
        for(int i=0;i<offsets.length;++i){
          positions[i][0]=posX+offsets[i][0];
          positions[i][1]=posY+offsets[i][1];
          positions[i][2]=posZ+offsets[i][2];
          //only ingnore "the top"...otherwise let throw index out of bound exception 
          if(positions[i][0]==voxels.length || 
             positions[i][1]==voxels[0].length || 
             positions[i][2]==voxels[0][0].length){
            return;
          }
        }

        int cubeindex = 0;

        cubeindex |= getForVertex(voxels,0) < level ? 1   : 0;
        cubeindex |= getForVertex(voxels,1) < level ? 2   : 0;
        cubeindex |= getForVertex(voxels,2) < level ? 4   : 0;
        cubeindex |= getForVertex(voxels,3) < level ? 8   : 0;
        cubeindex |= getForVertex(voxels,4) < level ? 16  : 0;
        cubeindex |= getForVertex(voxels,5) < level ? 32  : 0;
        cubeindex |= getForVertex(voxels,6) < level ? 64  : 0;
        cubeindex |= getForVertex(voxels,7) < level ? 128 : 0;

        /* Cube is entirely in/out of the surface */
        if (LookUpTables.Edges[cubeindex] == 0){
          return;
        }

    int mask=1; //it will go: 1,2,4,8,16,32,64,128,256,512,1024,2048

    for(int i=0;i<12;++i){
      if ((LookUpTables.Edges[cubeindex] & mask) != 0){
        int[] v=edges[i];
        int[] pos0=positions[v[0]];
        int[] pos1=positions[v[1]];
        float val0=getForVertex(voxels,v[0]);
        float val1=getForVertex(voxels,v[1]);
        faceVertices[i]=interpolate(pos0,val0, pos1,val1);
        //vertexEmiter.accept(interpolate(pos0,val0, pos1,val1));
      }
      mask= mask << 1;
    }

    /* Return the vertices for the correct triangles */
    for (int i=0;i<LookUpTables.Triangles[cubeindex].length;++i){
      //emit a clone, otherwise it may be modified
      vertexEmiter.accept(faceVertices[LookUpTables.Triangles[cubeindex][i]].clone()); 
    }

  }

  /*
     Linearly interpolate the position where an isosurface cuts
     an edge between two vertices, each with their own scalar value
  */


   /**
   *  Simple linear interpolation for calculating the vertices of the faces. 
   *  The function needs to be identical to the used in the MarchingSquare class, if
   *  if this is used to close the open sides of the voxel volume. 
   *  
   *  @param pos0     an array with the 3 indices in the voxel array corresponding to the first point
   *  @param val0     the value corresponding to the first point
   *  @param pos1     an array with the 3 indices in the voxel array corresponding to the second point
   *  @param val1     the value corresponding to the first point
   *  @return         an array of 3 floats corresponding to the interpolated point
   *  @todo   make interpolate into a class or lambda, so it can be injected in both 
   *  MarchingCube and MarchingSquare, so it is possible to have more control and be consistent in both              
  **/




  private float[] interpolate(int[] pos0,float val0, int[] pos1, float val1){
    float[] vertex = new float[3];

    //and MarchingSquare
    // int errorUlps=4;

    // if(almostEqual(val0,level,errorUlps)){
    //   vertex[0]=pos0[0];
    //   vertex[1]=pos0[1];
    //   vertex[2]=pos0[2];
    // }
    // if(almostEqual(val1,level,errorUlps)){
    //   vertex[0]=pos1[0];
    //   vertex[1]=pos1[1];
    //   vertex[2]=pos1[2];
    // }
    // if(almostEqual(val0,val1,errorUlps)){
    //   vertex[0]=pos0[0];
    //   vertex[1]=pos0[1];
    //   vertex[2]=pos0[2];
    // }
    // else{

    //   float mu = (level - val0) / (val1 - val0);
    //   vertex[0] = pos0[0] + mu * (pos1[0] - pos0[0]);
    //   vertex[1] = pos0[1] + mu * (pos1[1] - pos0[1]);
    //   vertex[2] = pos0[2] + mu * (pos1[2] - pos0[2]);
    // }

    float mu = (level - val0) / (val1 - val0);
    vertex[0] = pos0[0] + mu * (pos1[0] - pos0[0]);
    vertex[1] = pos0[1] + mu * (pos1[1] - pos0[1]);
    vertex[2] = pos0[2] + mu * (pos1[2] - pos0[2]);

    return vertex;
  }

  //comparison using ulps. Adapted from:
  //https://randomascii.wordpress.com/2012/02/25/comparing-floating-point-numbers-2012-edition/
  //a bit overkill, but better safe than sorry.
  // private static boolean almostEqual(float f0, float f1, int maxUlpsDiff){
  //   // Different signs means they do not match.
  //   if (Math.signum(f0) != Math.signum(f1)){
  //       // Check for equality to make sure +0==-0
  //     if (f0 == f1){
  //        return true;
  //      }
  //      return false;
  //   }
  //   // Find the difference in ULPs.
  //   int ulpsDiff = Math.abs(Float.floatToIntBits(f0) - Float.floatToIntBits(f1));
  //   if (ulpsDiff <= maxUlpsDiff){
  //     return true;
  //   }
  //   return false;
  // }

}
