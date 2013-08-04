/*
 *  Copyright (C) 2011 in-somnia
 * 
 *  This file is part of JAAD.
 * 
 *  JAAD is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU Lesser General Public License as 
 *  published by the Free Software Foundation; either version 3 of the 
 *  License, or (at your option) any later version.
 *
 *  JAAD is distributed in the hope that it will be useful, but WITHOUT 
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General 
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.jaad.aac.syntax;

interface ScaleFactorBands {

    /* scalefactor-band tables end with -1, so that an error can be detected
    by index[i+1] without an exception */
    int[] SWB_LONG_WINDOW_COUNT = {
            41, 41, 47, 49, 49, 51, 47, 47, 43, 43, 43, 40
    };
    int[] SWB_OFFSET_1024_96 = {
            0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52, 56,
            64, 72, 80, 88, 96, 108, 120, 132, 144, 156, 172, 188, 212, 240,
            276, 320, 384, 448, 512, 576, 640, 704, 768, 832, 896, 960, 1024,
            -1
    };
    int[] SWB_OFFSET_1024_64 = {
            0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52, 56,
            64, 72, 80, 88, 100, 112, 124, 140, 156, 172, 192, 216, 240, 268,
            304, 344, 384, 424, 464, 504, 544, 584, 624, 664, 704, 744, 784, 824,
            864, 904, 944, 984, 1024,
            -1
    };
    int[] SWB_OFFSET_1024_48 = {
            0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 48, 56, 64, 72,
            80, 88, 96, 108, 120, 132, 144, 160, 176, 196, 216, 240, 264, 292,
            320, 352, 384, 416, 448, 480, 512, 544, 576, 608, 640, 672, 704, 736,
            768, 800, 832, 864, 896, 928, 1024,
            -1
    };
    int[] SWB_OFFSET_1024_32 = {
            0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 48, 56, 64, 72,
            80, 88, 96, 108, 120, 132, 144, 160, 176, 196, 216, 240, 264, 292,
            320, 352, 384, 416, 448, 480, 512, 544, 576, 608, 640, 672, 704, 736,
            768, 800, 832, 864, 896, 928, 960, 992, 1024,
            -1
    };
    int[] SWB_OFFSET_1024_24 = {
            0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 52, 60, 68,
            76, 84, 92, 100, 108, 116, 124, 136, 148, 160, 172, 188, 204, 220,
            240, 260, 284, 308, 336, 364, 396, 432, 468, 508, 552, 600, 652, 704,
            768, 832, 896, 960, 1024,
            -1
    };
    int[] SWB_OFFSET_1024_16 = {
            0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 80, 88, 100, 112, 124,
            136, 148, 160, 172, 184, 196, 212, 228, 244, 260, 280, 300, 320, 344,
            368, 396, 424, 456, 492, 532, 572, 616, 664, 716, 772, 832, 896, 960, 1024,
            -1
    };
    int[] SWB_OFFSET_1024_8 = {
            0, 12, 24, 36, 48, 60, 72, 84, 96, 108, 120, 132, 144, 156, 172,
            188, 204, 220, 236, 252, 268, 288, 308, 328, 348, 372, 396, 420, 448,
            476, 508, 544, 580, 620, 664, 712, 764, 820, 880, 944, 1024,
            -1
    };
    int[][] SWB_OFFSET_LONG_WINDOW = {
            SWB_OFFSET_1024_96,
            SWB_OFFSET_1024_96,
            SWB_OFFSET_1024_64,
            SWB_OFFSET_1024_48,
            SWB_OFFSET_1024_48,
            SWB_OFFSET_1024_32,
            SWB_OFFSET_1024_24,
            SWB_OFFSET_1024_24,
            SWB_OFFSET_1024_16,
            SWB_OFFSET_1024_16,
            SWB_OFFSET_1024_16,
            SWB_OFFSET_1024_8
    };
    int[] SWB_SHORT_WINDOW_COUNT = {
            12, 12, 12, 14, 14, 14, 15, 15, 15, 15, 15, 15
    };
    int[] SWB_OFFSET_128_96 = {
            0, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 92, 128,
            -1
    };
    int[] SWB_OFFSET_128_64 = {
            0, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 92, 128,
            -1
    };
    int[] SWB_OFFSET_128_48 = {
            0, 4, 8, 12, 16, 20, 28, 36, 44, 56, 68, 80, 96, 112, 128,
            -1
    };
    int[] SWB_OFFSET_128_24 = {
            0, 4, 8, 12, 16, 20, 24, 28, 36, 44, 52, 64, 76, 92, 108, 128,
            -1
    };
    int[] SWB_OFFSET_128_16 = {
            0, 4, 8, 12, 16, 20, 24, 28, 32, 40, 48, 60, 72, 88, 108, 128,
            -1
    };
    int[] SWB_OFFSET_128_8 = {
            0, 4, 8, 12, 16, 20, 24, 28, 36, 44, 52, 60, 72, 88, 108, 128,
            -1
    };
    int[][] SWB_OFFSET_SHORT_WINDOW = {
            SWB_OFFSET_128_96,
            SWB_OFFSET_128_96,
            SWB_OFFSET_128_64,
            SWB_OFFSET_128_48,
            SWB_OFFSET_128_48,
            SWB_OFFSET_128_48,
            SWB_OFFSET_128_24,
            SWB_OFFSET_128_24,
            SWB_OFFSET_128_16,
            SWB_OFFSET_128_16,
            SWB_OFFSET_128_16,
            SWB_OFFSET_128_8
    };
}
