package com.ansen.gif;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Ansen on 2017/5/12 18:54.
 *
 * @E-mail: ansen360@126.com
 * @Blog: "http://blog.csdn.net/qq_25804863"
 * @Github: "https://github.com/ansen360"
 * @PROJECT_NAME: GIF
 * @PACKAGE_NAME: com.ansen.gif
 * @Description: LZW 压缩算法(减小了图象数据的大小)
 */
public class LZWEncoder {

    private static final int EOF = -1;
    private static final int BITS = 12;
    private static final int HSIZE = 5003; // 80% occupancy

    private int imgW, imgH;
    private int initCodeSize;
    private int remaining;
    private int curPixel;
    private int n_bits; // number of bits/code
    private int maxbits = BITS; // user settable max # bits/code
    private int maxcode; // maximum code, given n_bits
    private int maxmaxcode = 1 << BITS; // should NEVER generate this code
    private int hsize = HSIZE; // for dynamic table sizing
    private int free_ent = 0; // first unused entry
    private int g_init_bits;
    private int ClearCode;
    private int EOFCode;
    private int cur_accum = 0;
    private int cur_bits = 0;
    private int a_count;

    private int[] htab = new int[HSIZE];
    private int[] codetab = new int[HSIZE];
    private int masks[] = {
            0x0000,
            0x0001,
            0x0003,
            0x0007,
            0x000F,
            0x001F,
            0x003F,
            0x007F,
            0x00FF,
            0x01FF,
            0x03FF,
            0x07FF,
            0x0FFF,
            0x1FFF,
            0x3FFF,
            0x7FFF,
            0xFFFF};

    private byte[] pixAry;
    private byte[] accum = new byte[256];
    private boolean clear_flg = false;


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
        if (a_count >= 254)
            flush_char(outs);
    }

    // table clear for block compress
    void cl_block(OutputStream outs) throws IOException {
        cl_hash(hsize);
        free_ent = ClearCode + 2;
        clear_flg = true;

        output(ClearCode, outs);
    }

    // reset code table
    void cl_hash(int hsize) {
        for (int i = 0; i < hsize; ++i)
            htab[i] = -1;
    }

    void compress(int init_bits, OutputStream outs) throws IOException {
        int fcode;
        int i /* = 0 */;
        int c;
        int ent;
        int disp;
        int hsize_reg;
        int hshift;

        // Set up the globals:  g_init_bits - initial number of bits
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

        outer_loop:
        while ((c = nextPixel()) != EOF) {
            fcode = (c << maxbits) + ent;
            i = (c << hshift) ^ ent; // xor hashing

            if (htab[i] == fcode) {
                ent = codetab[i];
                continue;
            } else if (htab[i] >= 0) // non-empty slot
            {
                disp = hsize_reg - i; // secondary hash (after G. Knott)
                if (i == 0)
                    disp = 1;
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
            } else
                cl_block(outs);
        }
        // Put out the final code.
        output(ent, outs);
        output(EOFCode, outs);
    }

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

    private int nextPixel() {
        if (remaining == 0)
            return EOF;

        --remaining;

        byte pix = pixAry[curPixel++];

        return pix & 0xff;
    }

    void output(int code, OutputStream outs) throws IOException {
        cur_accum &= masks[cur_bits];

        if (cur_bits > 0)
            cur_accum |= (code << cur_bits);
        else
            cur_accum = code;

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
