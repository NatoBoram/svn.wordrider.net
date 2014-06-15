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
package net.sourceforge.jaad.aac.ps2;

interface HuffmanTables {

    int[][] HUFF_IID_DEFAULT_DF = {
            {1, 1, 0},
            {3, 1, -1},
            {3, 2, 1},
            {4, 0, -2},
            {4, 1, 2},
            {5, 12, -3},
            {5, 13, 3},
            {6, 28, -4},
            {6, 29, 4},
            {7, 60, -5},
            {7, 61, 5},
            {8, 124, -6},
            {8, 125, 6},
            {9, 252, 7},
            {10, 507, -8},
            {10, 508, 8},
            {10, 511, -7},
            {11, 1012, 10},
            {11, 1019, -9},
            {11, 1020, 9},
            {12, 2026, -11},
            {12, 2027, 11},
            {12, 2043, -10},
            {13, 4073, -12},
            {13, 4074, 12},
            {14, 8145, -14},
            {14, 8150, 14},
            {14, 8169, -13},
            {14, 8170, 13},
            {15, 16303, -15},
            {15, 16336, 15},
            {16, 32578, -17},
            {16, 32579, 17},
            {16, 32686, -16},
            {16, 32687, 16},
            {17, 65152, -21},
            {17, 65153, 21},
            {17, 65154, -19},
            {17, 65155, 19},
            {17, 65208, -18},
            {17, 65209, 18},
            {18, 130420, -26},
            {18, 130421, -25},
            {18, 130422, -28},
            {18, 130423, -27},
            {18, 130696, -22},
            {18, 130697, 22},
            {18, 130698, -24},
            {18, 130699, -23},
            {18, 130700, 25},
            {18, 130701, 26},
            {18, 130702, 23},
            {18, 130703, 24},
            {18, 130736, 29},
            {18, 130737, 30},
            {18, 130738, 27},
            {18, 130739, 28},
            {18, 130740, -30},
            {18, 130741, -29},
            {18, 130742, -20},
            {18, 130743, 20}
    };
    int[][] HUFF_IID_DEFAULT_DT = {
            {1, 1, 0},
            {2, 0, 1},
            {3, 3, -1},
            {5, 10, -2},
            {5, 11, 2},
            {6, 17, -3},
            {6, 18, 3},
            {7, 32, -4},
            {7, 33, 4},
            {8, 76, 5},
            {9, 154, -6},
            {9, 155, 6},
            {9, 159, -5},
            {10, 313, -7},
            {10, 314, 7},
            {11, 624, 9},
            {11, 632, -8},
            {11, 633, 8},
            {12, 1250, 11},
            {12, 1262, -10},
            {12, 1263, 10},
            {12, 1271, -9},
            {13, 2503, -13},
            {13, 2520, 13},
            {13, 2537, -12},
            {13, 2538, 12},
            {13, 2541, -11},
            {14, 5047, -15},
            {14, 5072, 15},
            {14, 5078, -14},
            {14, 5079, 14},
            {15, 10008, -21},
            {15, 10009, -20},
            {15, 10010, 18},
            {15, 10011, 19},
            {15, 10084, -19},
            {15, 10085, -18},
            {15, 10093, -17},
            {15, 10146, 17},
            {15, 10161, -16},
            {15, 10162, 16},
            {16, 20172, -26},
            {16, 20173, 26},
            {16, 20174, -28},
            {16, 20175, -27},
            {16, 20176, 29},
            {16, 20177, 30},
            {16, 20178, 27},
            {16, 20179, 28},
            {16, 20180, -30},
            {16, 20181, -29},
            {16, 20182, -25},
            {16, 20183, 25},
            {16, 20184, -24},
            {16, 20185, 24},
            {16, 20294, -23},
            {16, 20295, 23},
            {16, 20320, -22},
            {16, 20321, 22},
            {16, 20326, 20},
            {16, 20327, 21}
    };
    int[][] HUFF_IID_FINE_DF = {
            {1, 0, 0},
            {3, 4, 1},
            {3, 5, -1},
            {4, 12, 2},
            {4, 13, -2},
            {5, 28, 3},
            {5, 29, -3},
            {6, 60, -4},
            {6, 61, 4},
            {6, 62, 5},
            {7, 126, -5},
            {8, 254, 6},
            {9, 510, -6},
            {10, 1022, -7},
            {11, 2046, 7},
            {13, 8188, 8},
            {13, 8189, -8},
            {14, 16380, 9},
            {14, 16381, 10},
            {15, 32764, -9},
            {15, 32765, 11},
            {16, 65532, -10},
            {17, 131066, -11},
            {17, 131067, -14},
            {17, 131068, -13},
            {17, 131069, -12},
            {17, 131070, 12},
            {18, 262142, 13},
            {18, 262143, 14}
    };
    int[][] HUFF_IID_FINE_DT = {
            {1, 0, 0},
            {2, 2, -1},
            {3, 6, 1},
            {4, 14, -2},
            {5, 30, 2},
            {6, 62, -3},
            {7, 126, 3},
            {8, 254, -4},
            {9, 510, 4},
            {10, 1022, -5},
            {11, 2046, 5},
            {12, 4094, -6},
            {13, 8190, 6},
            {14, 16382, 7},
            {15, 32766, -7},
            {17, 131068, 8},
            {17, 131069, -8},
            {19, 524280, 9},
            {19, 524281, -14},
            {19, 524282, -13},
            {19, 524283, -12},
            {20, 1048568, -11},
            {20, 1048569, -10},
            {20, 1048570, -9},
            {20, 1048571, 10},
            {20, 1048572, 11},
            {20, 1048573, 12},
            {20, 1048574, 13},
            {20, 1048575, 14}
    };
    int[][] HUFF_ICC_DF = {
            {1, 0, 0},
            {2, 2, 1},
            {3, 6, -1},
            {4, 14, 2},
            {5, 30, -2},
            {6, 62, 3},
            {7, 126, -3},
            {8, 254, 4},
            {9, 510, 5},
            {10, 1022, -4},
            {11, 2046, 6},
            {12, 4094, -5},
            {13, 8190, 7},
            {14, 16382, -6},
            {14, 16383, -7}
    };
    int[][] HUFF_ICC_DT = {
            {1, 0, 0},
            {2, 2, 1},
            {3, 6, -1},
            {4, 14, 2},
            {5, 30, -2},
            {6, 62, 3},
            {7, 126, -3},
            {8, 254, 4},
            {9, 510, -4},
            {10, 1022, 5},
            {11, 2046, -5},
            {12, 4094, 6},
            {13, 8190, -6},
            {14, 16382, -7},
            {14, 16383, 7}
    };
    int[][] HUFF_IPD_DF = {
            {1, 1, 0},
            {3, 0, 1},
            {4, 2, 4},
            {4, 3, 5},
            {4, 4, 3},
            {4, 5, 6},
            {4, 6, 2},
            {4, 7, 7}
    };
    int[][] HUFF_IPD_DT = {
            {1, 1, 0},
            {3, 2, 1},
            {3, 3, 7},
            {4, 0, 5},
            {4, 2, 2},
            {4, 3, 6},
            {5, 2, 4},
            {5, 3, 3}
    };
    int[][] HUFF_OPD_DF = {
            {1, 1, 0},
            {3, 0, 7},
            {3, 1, 1},
            {4, 4, 3},
            {4, 5, 6},
            {4, 6, 2},
            {5, 14, 5},
            {5, 15, 4}
    };
    int[][] HUFF_OPD_DT = {
            {1, 1, 0},
            {3, 2, 1},
            {3, 3, 7},
            {4, 0, 5},
            {4, 1, 2},
            {4, 2, 6},
            {5, 6, 4},
            {5, 7, 3}
    };
}
