/*
 * Copyright (C) 2018 OBONO
 * http://d.hatena.ne.jp/OBONO/
 *
 * Based on J2ME Animated GIF encoder
 * http://www.jappit.com/blog/2008/12/04/j2me-animated-gif-encoder/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.obnsoft.arduboyemu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class GifEncoder {

    private static final int WIDTH = 128;
    private static final int HEIGHT = 64;
    private static final int PIXELS = WIDTH * HEIGHT;
    private static final int DELAY = 2; // frame delay (hundredths)
    private static final byte[] PALETTE = new byte[] { 0, 0, 0, -1, -1, -1 };
    private static final int COLOR_DEPTH = 1; // color depth
    private static final int PAL_SIZE = 0; // palette size (bits-1)

    private File mWorkFile;
    private OutputStream mWorkStream;
    private boolean mIsStarted = false; // ready to output frames
    private boolean mIsFirstFrame = true;

    /**
     * Initiates GIF file creation.
     *
     * @return false if initial write failed.
     */
    public boolean start(File file) {
        if (mIsStarted) {
            return false;
        }
        try {
            mWorkFile = file;
            mWorkStream = new FileOutputStream(file);
            writeHeader(mWorkStream); // header
            mIsStarted = true;
            mIsFirstFrame = true;
        } catch (IOException e) {
            e.printStackTrace();
            mWorkFile = null;
            mWorkStream = null;
        }
        return mIsStarted;
    }

    /**
     * Adds next GIF frame. The frame is not written immediately, but is
     * actually deferred until the next frame is received so that timing data
     * can be inserted. Invoking <code>finish()</code> flushes all frames.
     *
     * @return true if successful.
     */
    public boolean addFrame(int[] pixels) {
        if (!mIsStarted || pixels == null || pixels.length != PIXELS) {
            return false;
        }
        boolean ret = false;
        try {
            if (mIsFirstFrame) {
                writeLSD(mWorkStream); // logical screen descriptior
                writePalette(mWorkStream); // global color table
                mIsFirstFrame = false;
            }
            byte[] indexedPixels = analyzePixels(pixels); // build map pixels
            writeGraphicCtrlExt(mWorkStream); // write graphic control extension
            writeImageBlock(mWorkStream, indexedPixels); // write image block
            ret = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    /**
     * Flushes any pending data and closes output file.
     */
    public boolean finish(File file) {
        if (!mIsStarted) {
            return false;
        }
        boolean ret = false;
        try {
            writeTrailer(mWorkStream); // gif trailer
            mWorkStream.close();
            mWorkFile.renameTo(file);
            ret = true;
        } catch (IOException e) {
            e.printStackTrace();
            mWorkFile.delete();
        }

        // reset for subsequent use
        mWorkFile = null;
        mWorkStream = null;
        mIsStarted = false;

        return ret;
    }

    public boolean oneShot(File file, int[] pixels) {
        if (pixels == null || pixels.length != PIXELS) {
            return false;
        }
        boolean ret = false;
        try {
            FileOutputStream out = new FileOutputStream(file);
            writeHeader(out); // header
            writeLSD(out); // logical screen descriptior
            writePalette(out); // global color table
            byte[] indexedPixels = analyzePixels(pixels); // build map pixels
            writeImageBlock(out, indexedPixels); // write image block
            writeTrailer(out); // gif trailer
            out.close();
            ret = true;
        } catch (IOException e) {
            e.printStackTrace();
            file.delete();
        }
        return ret;
    }

    /**
     * Analyzes image colors and creates color map.
     */
    private byte[] analyzePixels(int[] pixels) {
        byte[] indexedPixels = new byte[PIXELS];
        for (int i = 0; i < PIXELS; i++) {
            int c = pixels[i];
            int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
            boolean isWhite = ((306 * r + 601 * g + 117 * b) >= 512);
            indexedPixels[i] = (byte) (isWhite ? 1 : 0);
        }
        return indexedPixels;
    }

    /**
     * Writes GIF Header
     */
    private void writeHeader(OutputStream out) throws IOException {
        writeString(out, "GIF89a"); // header
    }

    /**
     * Writes Logical Screen Descriptor
     */
    private void writeLSD(OutputStream out) throws IOException {
        // logical screen size
        writeShort(out, WIDTH);
        writeShort(out, HEIGHT);
        // packed fields
        out.write(0x80 | // 1 : global color table flag = 1 (gct used)
                ((COLOR_DEPTH - 1) << 4) | // 2-4 : color resolution
                0x08 | // 5 : gct sort flag = 1
                PAL_SIZE); // 6-8 : gct size

        out.write(0); // background color index
        out.write(0); // pixel aspect ratio - assume 1:1
    }

    /**
     * Writes color table
     */
    private void writePalette(OutputStream out) throws IOException {
        out.write(PALETTE);
    }

    /**
     * Writes Graphic Control Extension
     */
    private void writeGraphicCtrlExt(OutputStream out) throws IOException {
        out.write(0x21); // extension introducer
        out.write(0xf9); // GCE label
        out.write(4); // data block size

        // packed fields
        out.write(0 | // 1:3 reserved
                0 | // 4:6 disposal
                0 | // 7 user input = 0 (none)
                0); // 8 transparency flag = 0 (none)

        writeShort(out, DELAY); // delay x 1/100 sec
        out.write(0); // transparent color index = 0
        out.write(0); // block terminator
    }

    /**
     * Writes Image Block
     */
    private void writeImageBlock(OutputStream out, byte[] indexedPixels) throws IOException {
        writeImageDesc(out); // image descriptor
        LZWEncoder encoder = new LZWEncoder(WIDTH, HEIGHT, indexedPixels, COLOR_DEPTH);
        encoder.encode(out); // encoded pixel data
    }

    /**
     * Writes Image Descriptor
     */
    private void writeImageDesc(OutputStream out) throws IOException {
        out.write(0x2c); // image separator
        writeShort(out, 0); // image position x,y = 0,0
        writeShort(out, 0);
        writeShort(out, WIDTH); // image size = 128x64
        writeShort(out, HEIGHT);
        out.write(0); // no LCT - GCT is used
    }

    /**
     * Writes GIF Trailer and Flush
     */
    private void writeTrailer(OutputStream out) throws IOException {
        out.write(0x3b); // gif trailer
        out.flush();
    }

    /**
     * Write 16-bit value to output stream, LSB first
     */
    private void writeShort(OutputStream out, int value) throws IOException {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }

    /**
     * Writes string to output stream
     */
    private void writeString(OutputStream out, String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            out.write((byte) s.charAt(i));
        }
    }

}

// ==============================================================================
// Adapted from Jef Poskanzer's Java port by way of J. M. G. Elliott.
// K Weiner 12/00

class LZWEncoder {

    private static final int EOF = -1;

    private int imgW, imgH;

    private byte[] pixAry;

    private int initCodeSize;

    private int remaining;

    private int curPixel;

    // GIFCOMPR.C - GIF Image compression routines
    //
    // Lempel-Ziv compression based on 'compress'. GIF modifications by
    // David Rowley (mgardi@watdcsu.waterloo.edu)

    // General DEFINEs

    static final int BITS = 12;

    static final int HSIZE = 5003; // 80% occupancy

    // GIF Image compression - modified 'compress'
    //
    // Based on: compress.c - File compression ala IEEE Computer, June 1984.
    //
    // By Authors: Spencer W. Thomas (decvax!harpo!utah-cs!utah-gr!thomas)
    // Jim McKie (decvax!mcvax!jim)
    // Steve Davies (decvax!vax135!petsd!peora!srd)
    // Ken Turkowski (decvax!decwrl!turtlevax!ken)
    // James A. Woods (decvax!ihnp4!ames!jaw)
    // Joe Orost (decvax!vax135!petsd!joe)

    int n_bits; // number of bits/code

    int maxbits = BITS; // user settable max # bits/code

    int maxcode; // maximum code, given n_bits

    int maxmaxcode = 1 << BITS; // should NEVER generate this code

    int[] htab = new int[HSIZE];

    int[] codetab = new int[HSIZE];

    int hsize = HSIZE; // for dynamic table sizing

    int free_ent = 0; // first unused entry

    // block compression parameters -- after all codes are used up,
    // and compression rate changes, start over.
    boolean clear_flg = false;

    // Algorithm: use open addressing double hashing (no chaining) on the
    // prefix code / next character combination. We do a variant of Knuth's
    // algorithm D (vol. 3, sec. 6.4) along with G. Knott's relatively-prime
    // secondary probe. Here, the modular division first probe is gives way
    // to a faster exclusive-or manipulation. Also do block compression with
    // an adaptive reset, whereby the code table is cleared when the compression
    // ratio decreases, but after the table fills. The variable-length output
    // codes are re-sized at this point, and a special CLEAR code is generated
    // for the decompressor. Late addition: construct the table according to
    // file size for noticeable speed improvement on small files. Please direct
    // questions about this implementation to ames!jaw.

    int g_init_bits;

    int ClearCode;

    int EOFCode;

    // output
    //
    // Output the given code.
    // Inputs:
    // code: A n_bits-bit integer. If == -1, then EOF. This assumes
    // that n_bits =< wordsize - 1.
    // Outputs:
    // Outputs code to the file.
    // Assumptions:
    // Chars are 8 bits long.
    // Algorithm:
    // Maintain a BITS character long buffer (so that 8 codes will
    // fit in it exactly). Use the VAX insv instruction to insert each
    // code in turn. When the buffer fills up empty it and start over.

    int cur_accum = 0;

    int cur_bits = 0;

    int masks[] = { 0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F, 0x00FF, 0x01FF,
            0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF };

    // Number of characters so far in this 'packet'
    int a_count;

    // Define the storage for the packet accumulator
    byte[] accum = new byte[256];

    // ----------------------------------------------------------------------------
    LZWEncoder(int width, int height, byte[] pixels, int color_depth) {
        imgW = width;
        imgH = height;
        pixAry = pixels;
        initCodeSize = Math.max(2, color_depth);
    }

    // Add a character to the end of the current packet, and if it is 254
    // characters, flush the packet to disk.
    void char_out(byte c, OutputStream outs) throws IOException {
        accum[a_count++] = c;
        if (a_count >= 254) {
            flush_char(outs);
        }
    }

    // Clear out the hash table

    // table clear for block compress
    void cl_block(OutputStream outs) throws IOException {
        cl_hash(hsize);
        free_ent = ClearCode + 2;
        clear_flg = true;

        output(ClearCode, outs);
    }

    // reset code table
    void cl_hash(int hsize) {
        for (int i = 0; i < hsize; ++i) {
            htab[i] = -1;
        }
    }

    void compress(int init_bits, OutputStream outs) throws IOException {
        int fcode;
        int i /* = 0 */;
        int c;
        int ent;
        int disp;
        int hsize_reg;
        int hshift;

        // Set up the globals: g_init_bits - initial number of bits
        g_init_bits = init_bits;

        // Set up the necessary values
        clear_flg = false;
        n_bits = g_init_bits;
        maxcode = MAXCODE(n_bits);

        ClearCode = 1 << (init_bits - 1);
        EOFCode = ClearCode + 1;
        free_ent = ClearCode + 2;

        a_count = 0; // clear packet

        ent = nextPixel();

        hshift = 0;
        for (fcode = hsize; fcode < 65536; fcode *= 2)
            ++hshift;
        hshift = 8 - hshift; // set hash code range bound

        hsize_reg = hsize;
        cl_hash(hsize_reg); // clear hash table

        output(ClearCode, outs);

        outer_loop: while ((c = nextPixel()) != EOF) {
            fcode = (c << maxbits) + ent;
            i = (c << hshift) ^ ent; // xor hashing

            if (htab[i] == fcode) {
                ent = codetab[i];
                continue;
            } else if (htab[i] >= 0) { // non-empty slot
                disp = hsize_reg - i; // secondary hash (after G. Knott)
                if (i == 0) {
                    disp = 1;
                }
                do {
                    if ((i -= disp) < 0)
                        i += hsize_reg;

                    if (htab[i] == fcode) {
                        ent = codetab[i];
                        continue outer_loop;
                    }
                } while (htab[i] >= 0);
            }
            output(ent, outs);
            ent = c;
            if (free_ent < maxmaxcode) {
                codetab[i] = free_ent++; // code -> hashtable
                htab[i] = fcode;
            } else {
                cl_block(outs);
            }
        }
        // Put out the final code.
        output(ent, outs);
        output(EOFCode, outs);
    }

    // ----------------------------------------------------------------------------
    void encode(OutputStream os) throws IOException {
        os.write(initCodeSize); // write "initial code size" byte

        remaining = imgW * imgH; // reset navigation variables
        curPixel = 0;

        compress(initCodeSize + 1, os); // compress and write the pixel data

        os.write(0); // write block terminator
    }

    // Flush the packet to disk, and reset the accumulator
    void flush_char(OutputStream outs) throws IOException {
        if (a_count > 0) {
            outs.write(a_count);
            outs.write(accum, 0, a_count);
            a_count = 0;
        }
    }

    final int MAXCODE(int n_bits) {
        return (1 << n_bits) - 1;
    }

    // ----------------------------------------------------------------------------
    // Return the next pixel from the image
    // ----------------------------------------------------------------------------
    private int nextPixel() {
        if (remaining == 0) {
            return EOF;
        }

        --remaining;

        byte pix = pixAry[curPixel++];

        return pix & 0xff;
    }

    void output(int code, OutputStream outs) throws IOException {
        cur_accum &= masks[cur_bits];

        if (cur_bits > 0) {
            cur_accum |= (code << cur_bits);
        } else {
            cur_accum = code;
        }
        cur_bits += n_bits;

        while (cur_bits >= 8) {
            char_out((byte) (cur_accum & 0xff), outs);
            cur_accum >>= 8;
            cur_bits -= 8;
        }

        // If the next entry is going to be too big for the code size,
        // then increase it, if possible.
        if (free_ent > maxcode || clear_flg) {
            if (clear_flg) {
                maxcode = MAXCODE(n_bits = g_init_bits);
                clear_flg = false;
            } else {
                ++n_bits;
                if (n_bits == maxbits)
                    maxcode = maxmaxcode;
                else
                    maxcode = MAXCODE(n_bits);
            }
        }

        if (code == EOFCode) {
            // At EOF, write the rest of the buffer.
            while (cur_bits > 0) {
                char_out((byte) (cur_accum & 0xff), outs);
                cur_accum >>= 8;
                cur_bits -= 8;
            }

            flush_char(outs);
        }
    }
}