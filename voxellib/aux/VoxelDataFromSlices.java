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
 * A Class that extends VoxelDataAdapter in order to easily fill it with
 * two dimensional slices of data stacked on each other.
 *  @author Pablo Miranda Carranza
 */

public class VoxelDataFromSlices extends VoxelDataAdapter{
    int currentSlice;	

    /**
     * Constructor.
     * @param processing    the PAplet used for drawing
     * @param w             width in voxels 
     * @param h             height in voxels 
     * @param d             depth in voxels 
     */
    public VoxelDataFromSlices(PApplet processing, int w, int h, int d) {
        super(processing, w,h,d);
        currentSlice=0;
    }


    /**
     * Used to set values of individual voxels at the current level
     * @param x     the x coordinate of the voxel to set
     * @param y     the y coordinate of the voxel to set
     * @param val   the value to set the voxel to
     */
    public void set(int x,int y, float val){
        //just avoid out of bound errors
        if(currentSlice<dd){
            if(x>=0 && x<dw && y>=0 && y<dh){
                data[x][y][currentSlice]=val;
            }
        }
    }
    /**
     * increases the level at which calls to set() will operate. 
     * @return boolean  if it is not possible to increase the level (because it has reached the maximum) it returns false.
     */
    public boolean increaseLevel(){ 
        if(currentSlice<dd){
            ++currentSlice;
        }
        return currentSlice<=dd; //so it returns also true when it overflows.
    }

    /**
     * It resets the level at which calls to set() work to 0.
     */
    public void resetLevel(){
        currentSlice=0;
    }

}