/*
 * @(#)ByteBufferAs-X-Buffer.java	1.14 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// -- This file was mechanically generated: Do not edit! -- //

package java.nio;


class ByteBufferAsCharBufferRL			// package-private
    extends ByteBufferAsCharBufferL
{








    ByteBufferAsCharBufferRL(ByteBuffer bb) {	// package-private












	super(bb);

    }

    ByteBufferAsCharBufferRL(ByteBuffer bb,
				     int mark, int pos, int lim, int cap,
				     int off)
    {





	super(bb, mark, pos, lim, cap, off);

    }

    public CharBuffer slice() {
	int pos = this.position();
	int lim = this.limit();
	assert (pos <= lim);
	int rem = (pos <= lim ? lim - pos : 0);
	int off = (pos << 1) + offset;
	return new ByteBufferAsCharBufferRL(bb, -1, 0, rem, rem, off);
    }

    public CharBuffer duplicate() {
	return new ByteBufferAsCharBufferRL(bb,
						    this.markValue(),
						    this.position(),
						    this.limit(),
						    this.capacity(),
						    offset);
    }

    public CharBuffer asReadOnlyBuffer() {








	return duplicate();

    }

















    public CharBuffer put(char x) {




	throw new ReadOnlyBufferException();

    }

    public CharBuffer put(int i, char x) {




	throw new ReadOnlyBufferException();

    }

    public CharBuffer compact() {
















	throw new ReadOnlyBufferException();

    }

    public boolean isDirect() {
	return bb.isDirect();
    }

    public boolean isReadOnly() {
	return true;
    }



    public String toString(int start, int end) {
	if ((end > limit()) || (start > end))
	    throw new IndexOutOfBoundsException();
	try {
	    int len = end - start;
	    char[] ca = new char[len];
	    CharBuffer cb = CharBuffer.wrap(ca);
	    CharBuffer db = this.duplicate();
	    db.position(start);
	    db.limit(end);
	    cb.put(db);
	    return new String(ca);
	} catch (StringIndexOutOfBoundsException x) {
	    throw new IndexOutOfBoundsException();
	}
    }


    // --- Methods to support CharSequence ---

    public CharSequence subSequence(int start, int end) {
	int len = length();
	int pos = position();
	assert (pos <= len);
	pos = (pos <= len ? pos : len);

	if ((start < 0) || (end > len) || (start > end))
	    throw new IndexOutOfBoundsException();
	int sublen = end - start;
 	int off = offset + ((pos + start) << 1);
	return new ByteBufferAsCharBufferRL(bb, -1, 0, sublen, sublen, off);
    }




    public ByteOrder order() {




	return ByteOrder.LITTLE_ENDIAN;

    }

}
