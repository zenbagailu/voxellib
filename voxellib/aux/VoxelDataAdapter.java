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
 *  part of the voxellib package, library for processing voxels.
 *
 *  @author Pablo Miranda Carranza
 */


package voxellib.aux;
import processing.core.*; //for the QUADS definition and drawing
import voxellib.Mesh;

/**
 * A Class providing a simple apapter to a 3 dimensional array of floats and 
 * a voxellib.Mesh.
 *  @author Pablo Miranda Carranza
 */

public class VoxelDataAdapter{

    float[][][] data;
    int dw,dh,dd;
    Mesh mesh; //a mesh object from voxelLib...


    /**
     * The Constructor.
     * @param processing    the PAplet used for drawing
     * @param w             width in voxels 
     * @param h             height in voxels 
     * @param d             depth in voxels 
     */
    public VoxelDataAdapter(PApplet processing, int w, int h, int d) {
        dw=w;
        dh=h;
        dd=d;
        data=new float[w][h][d];
        mesh=new Mesh(processing); 
    }

    /**
     * Used to set values of individual voxels
     * @param x     the x coordinate of the voxel to set
     * @param y     the y coordinate of the voxel to set
     * @param z     the z coordinate of the voxel to set
     * @param val   the value to set the voxel to
     */
    public void set(int x,int y, int z, float val){
        //just avoid out of bound errors
        if(x>=0 && x<dw && y>=0 && y<dh && z>=0 && z<dd){
            data[x][y][z]=val;
        } else{
            System.out.println("error");
        } 
    }

    /**
     * Calculate the mesh corresponding to the level value.
     * @param level     the level (or isolevel) used to calculate the surface using the marching cubes algorithm
     */
    
    public void calculate(float level){
        mesh.makeFromVoxels(data, 1.0f ,level);
    }

    /**
     * calls the draw() method of the voxellib.Mesh
     */
    public void draw(){
        mesh.draw();
    }

    /**
     * calls the saveAsStl() method of the voxellib.Mesh
     */

    public void saveAsStl(String fileName){
        mesh.saveAsStl(fileName);
    }
}