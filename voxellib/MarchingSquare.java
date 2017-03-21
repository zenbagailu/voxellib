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
 *  voxellib package, library for processing voxels
 *
 *	@author Pablo Miranda Carranza
 */

 package voxellib;

 import java.util.function.*;

 /**
 *  An implementation of the marching squares using look-up tables. It can either produce
 *  triangulations of the area above the given level, or just lines defining the boundary.
 *	@author Pablo Miranda Carranza
 */

 public class MarchingSquare{


	private static final int[][] Table={
	{}, //element 0, all bellow
	{0,1, 1,1, 1,0, 1,1},
	{1,1, 1,0, 0,0, 1,0},
	{0,1, 1,1, 0,0, 1,0},
	{0,0, 0,1, 1,1, 0,1},
	{0,0, 0,1, 1,0, 1,1},
	{0,0, 0,1, 0,0, 1,0,  1,1, 1,0, 1,1, 0,1},//they have 16 values
	{0,0, 0,1, 0,0, 1,0},
	{1,0, 0,0, 0,1, 0,0},
	{1,0, 0,0, 1,0, 1,1,  0,1, 1,1, 0,1, 0,0},
	{1,1, 1,0, 0,1, 0,0},
	{0,1, 1,1, 0,1, 0,0},
	{1,0, 0,0, 1,1, 0,1},
	{1,0, 0,0, 1,0, 1,1},
	{1,1, 1,0, 1,1, 0,1},
	{} //element 15, all above
	};

	//These are for producing the triangulations.
	//The last 4 vertices are calculated ny the marching square algorithm
	//They correspond to the 1st and 2nd vertices, and 3rd and 4th in cases
	//6 and 9, calculated using Table. 

	private final float[][] vertices={
		{0,0},{1,0},{0,1},{1,1},
		{0,0},{0,0},{0,0},{0,0} //these will be calculated and set on for triangulations
	};
	//This table describes the faces corresponding to each of the 16 cases
	//and it matches (and it depends of) the results and order of "Table".
	private static final int[][] Triangulations={
		{},
		{4,3,5},
		{4,1,5},
		{4,3,1, 4,1,5},
		{4,2,5},
		{4,2,5, 2,3,5},
		{4,2,7, 4,7,6, 5,4,6, 5,6,1},
		{4,2,3, 4,3,5, 3,1,5},
		{4,0,5},
		{4,6,5, 5,6,3, 4,7,6, 4,0,7},
		{4,1,5, 1,0,5},
		{5,1,0, 5,3,1, 5,4,3},
		{4,0,2, 2,5,4},
		{4,0,2, 4,2,5, 5,2,3},
		{4,1,0, 4,0,5, 0,2,5},
		{0,2,1, 2,3,1}
	};

	public enum Winding{Clockwise, CounterClockwise};


	private float level;
  	private Consumer< float[]>  vertexEmiter;

  /**
   * The constructor takes a functional interface of type Consumer{@literal <}float[]{@literal >}.
   * so a lambda expression can be passed to it, to process the vertices.
   * @param   level         the level we want to use to define the boundaries.
   * @param   vertexEmiter  possibly a lambda expresion for processing the vertices 
  **/


	public MarchingSquare (float level, Consumer< float[]>  vertexEmiter){
    	this.level=level;
    	this.vertexEmiter=vertexEmiter;
	}

 /**
   * It calculates the boundary and triangulates the area inside or larger than the given level. It reports
   * the vertices of each triangle using the Consumer{@literal <}float[]{@literal >} vertexEmiter field, with the 
   * corresponding winding given as a parameter.
   * @param   vals  an 2D array of floats defining the 4 corners of the square to consider.
   * @param   winding  either  MarchingSquare.Winding.Clockwise or MarchingSquare.Winding.CounterClockwise
  **/

	public void calculateFaces(float[][] vals, Winding winding){

		int squareType=calculateType(vals);
		calculateVertices(vals, squareType);

        //emit vertices of faces
        for(int i=0;i<Triangulations[squareType].length; ++i){
        	if(winding==Winding.Clockwise){
        		vertexEmiter.accept(vertices[Triangulations[squareType][i]].clone());
        	}else{
        		vertexEmiter.accept(vertices[Triangulations[squareType][Triangulations[squareType].length-(i+1)]].clone());
        	}
        	
        }
	}

  /**
   * It calculates the boundary and emits the vertices forming line segments through the the 
   * Consumer{@literal <}float[]{@literal >} vertexEmiter field.
   * @param   vals  an 2D array of floats defining the 4 corners of the square to consider.
  **/

	public void calculateLines(float[][] vals){

		int squareType=calculateType(vals);
		calculateVertices(vals, squareType);

        //emit vertices of lines
        for(int i=0;i<Table[squareType].length/8;++i){
        	vertexEmiter.accept(vertices[4+i].clone());
        	vertexEmiter.accept(vertices[5+i].clone());
        }
	}

	private int calculateType(float[][] vals){
		return
		(vals[0][0] > level ? 8:0)  | 
		(vals[0][1] > level ? 4:0)  | 
		(vals[1][0] > level ? 2:0)  | 
		(vals[1][1] > level ? 1:0);
	}

	private void calculateVertices(float[][] vals, int SqType){

		for(int i=0;i<Table[SqType].length;i+=8){ // for each resulting line...
      
            //because of the table valA is going to be always the lowest
            int posAx=Table[SqType][i];
            int posAy=Table[SqType][i+1];

            int posBx=Table[SqType][i+2];
            int posBy=Table[SqType][i+3];

            float valA=vals[posAx][posAy];
            float valB=vals[posBx][posBy];

            float inters1=(level-valA)/(valB-valA);

            float pt1x=posAx+(posBx-posAx)*inters1; 
            float pt1y=posAy+(posBy-posAy)*inters1;

            //the talest (b) - the lowest, will always give the direction
           
			int posCx=Table[SqType][i+4];
            int posCy=Table[SqType][i+5];

            int posDx=Table[SqType][i+6];
            int posDy=Table[SqType][i+7];

            float valC=vals[posCx][posCy];
            float valD=vals[posDx][posDy];

            float inters2=(level-valC)/(valD-valC);

            float pt2x=posCx+(posDx-posCx)*inters2; 
            float pt2y=posCy+(posDy-posCy)*inters2;

            vertices[4+i/8][0]=pt1x;
            vertices[4+i/8][1]=pt1y;
            vertices[5+i/8][0]=pt2x;
            vertices[5+i/8][1]=pt2y;
        }
	}

}