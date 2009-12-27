package cz.vity.freerapid.plugins.services.indowebster;

import org.apache.commons.codec.binary.Base64;

import java.util.Arrays;

/**
 * AES decriptor
 *
 * @author JPEXS
 *         <p/>
 *         Based on JavaScript implementation by Chris Veness
 */
public final class AES {

    private static final int SBOX[] = new int[]{0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76, 0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0, 0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15, 0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75, 0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84, 0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf, 0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8, 0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2, 0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73, 0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb, 0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79, 0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08, 0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a, 0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e, 0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf, 0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16};
    private static final int RCON[][] = new int[][]{{0x00, 0x00, 0x00, 0x00}, {0x01, 0x00, 0x00, 0x00}, {0x02, 0x00, 0x00, 0x00}, {0x04, 0x00, 0x00, 0x00}, {0x08, 0x00, 0x00, 0x00}, {0x10, 0x00, 0x00, 0x00}, {0x20, 0x00, 0x00, 0x00}, {0x40, 0x00, 0x00, 0x00}, {0x80, 0x00, 0x00, 0x00}, {0x1b, 0x00, 0x00, 0x00}, {0x36, 0x00, 0x00, 0x00}};

    private static int[] cipher(int[] input, int[][] w) {
        int nb = 4;
        int nr = w.length / nb - 1;
        int state[][] = new int[4][4];
        for (int i = 0; i < 4 * nb; i++) {
            state[i % 4][(int) Math.floor(i / 4)] = input[i];
        }
        state = addRoundKey(state, w, 0, nb);
        for (int round = 1; round < nr; round++) {
            state = subBytes(state, nb);
            state = shiftRows(state, nb);
            state = mixColumns(state, nb);
            state = addRoundKey(state, w, round, nb);
        }
        state = subBytes(state, nb);
        state = shiftRows(state, nb);
        state = addRoundKey(state, w, nr, nb);
        int output[] = new int[4 * nb];
        for (int i = 0; i < 4 * nb; i++) {
            output[i] = state[i % 4][(int) Math.floor(i / 4)];
        }
        return output;
    }

    private static int[][] subBytes(int[][] s, int Nb) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < Nb; c++) {
                s[r][c] = SBOX[s[r][c]];
            }
        }
        return s;
    }

    private static int[][] shiftRows(int[][] s, int Nb) {
        int t[] = new int[4];
        for (int r = 1; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                t[c] = s[r][(c + r) % Nb];
            }
            System.arraycopy(t, 0, s[r], 0, 4);
        }
        return s;
    }

    private static int[][] mixColumns(int[][] s, int Nb) {
        for (int c = 0; c < 4; c++) {
            int a[] = new int[4];
            int b[] = new int[4];
            for (int i = 0; i < 4; i++) {
                a[i] = s[i][c];
                b[i] = (s[i][c] & 0x80) != 0 ? s[i][c] << 1 ^ 0x011b : s[i][c] << 1;
            }
            s[0][c] = b[0] ^ a[1] ^ b[1] ^ a[2] ^ a[3];
            s[1][c] = a[0] ^ b[1] ^ a[2] ^ b[2] ^ a[3];
            s[2][c] = a[0] ^ a[1] ^ b[2] ^ a[3] ^ b[3];
            s[3][c] = a[0] ^ b[0] ^ a[1] ^ a[2] ^ b[3];
        }
        return s;
    }

    private static int[][] addRoundKey(int[][] state, int[][] w, int rnd, int Nb) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < Nb; c++) {
                state[r][c] ^= w[rnd * 4 + c][r];
            }
        }
        return state;
    }

    private static int[][] keyExpansion(int[] key) {
        int Nb = 4;
        int Nk = key.length / 4;
        int Nr = Nk + 6;
        int w[][] = new int[Nb * (Nr + 1)][4];
        int temp[] = new int[4];
        for (int i = 0; i < Nk; i++) {
            int r[] = new int[]{key[4 * i], key[4 * i + 1], key[4 * i + 2], key[4 * i + 3]};
            w[i] = r;
        }
        for (int i = Nk; i < (Nb * (Nr + 1)); i++) {
            w[i] = new int[4];
            System.arraycopy(w[i - 1], 0, temp, 0, 4);
            if (i % Nk == 0) {
                temp = subWord(rotWord(temp));
                for (int t = 0; t < 4; t++) {
                    temp[t] ^= RCON[i / Nk][t];
                }
            } else if (Nk > 6 && i % Nk == 4) {
                temp = subWord(temp);
            }
            for (int t = 0; t < 4; t++) {
                w[i][t] = w[i - Nk][t] ^ temp[t];
            }
        }
        return w;
    }

    private static int[] subWord(int w[]) {
        for (int i = 0; i < 4; i++) {
            w[i] = SBOX[w[i]];
        }
        return w;
    }

    private static int[] rotWord(int w[]) {
        int tmp = w[0];
        System.arraycopy(w, 1, w, 0, 3);
        w[3] = tmp;
        return w;
    }


    public static String AESDecryptCtr(String ciphertextString, String password, int nBits) {
        int blockSize = 16;
        if (!(nBits == 128 || nBits == 192 || nBits == 256)) {
            throw new IllegalArgumentException("Only 128 or 192 or 256 bits are allowed");
        }
        byte ciphertextBytes[] = Base64.decodeBase64(ciphertextString.getBytes());
        int ciphertext[] = new int[ciphertextBytes.length];
        for (int i = 0; i < ciphertext.length; i++) {
            ciphertext[i] = 0xff & ciphertextBytes[i];
        }
        int nBytes = nBits / 8;
        int pwBytes[] = new int[nBytes];
        for (int i = 0; i < nBytes; i++) {
            pwBytes[i] = i >= password.length() ? 0 : password.codePointAt(i);
        }
        int key1[] = cipher(pwBytes, keyExpansion(pwBytes));
        int key[] = Arrays.copyOf(key1, key1.length + (nBytes - 16));
        System.arraycopy(key1, 0, key, key1.length, nBytes - 16);

        int counterBlock[] = new int[16];
        System.arraycopy(ciphertext, 0, counterBlock, 0, 8);

        int[][] keySchedule = keyExpansion(key);

        int nBlocks = (int) Math.ceil((ciphertext.length - 8) / (double) blockSize);
        int ct[][] = new int[nBlocks][];
        for (int b = 0; b < nBlocks; b++) {
            int sec = 8 + b * blockSize + blockSize;
            if (sec > ciphertext.length) {
                sec = ciphertext.length;
            }
            ct[b] = Arrays.copyOfRange(ciphertext, 8 + b * blockSize, sec);
        }

        String plaintext = "";
        for (int b = 0; b < nBlocks; b++) {
            for (int c = 0; c < 4; c++) {
                counterBlock[15 - c] = ((b) >>> c * 8) & 0xff;
            }
            for (int c = 0; c < 4; c++) {
                long l1 = (long) (((b + 1) / (double) 0x100000000L) - 1);
                long l2 = (l1 >>> c * 8);
                counterBlock[15 - c - 4] = (int) (l2 & 0xff);
            }
            int cipherCntr[] = cipher(counterBlock, keySchedule);
            byte plaintxtByte[] = new byte[ct[b].length];
            for (int i = 0; i < ct[b].length; i++) {
                plaintxtByte[i] = (byte) (cipherCntr[i] ^ ct[b][i]);
            }
            plaintext += new String(plaintxtByte);
        }
        return plaintext;
    }
}
