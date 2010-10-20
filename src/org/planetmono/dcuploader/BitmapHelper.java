/* Copyright (c) 2010 Park "segfault" Joon-Kyu
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.planetmono.dcuploader;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class BitmapHelper {
	enum Axis { Horizontal, Vertical };
	
	/* Resize bitmap (keep aspect ratio) */
	static Bitmap getResizedBitmap(Bitmap orig, int pixels, Axis axis, int orientation) {
		int ow = orig.getWidth();
		int oh = orig.getHeight();
		
		int nw, nh;
		
		if (axis == Axis.Horizontal) {
			nw = pixels;
			nh = (int)(nw * ((float)oh / ow));
		} else {
			nh = pixels;
			nw = (int)(nh * ((float)ow / oh));
		}
		
		float sw = (float)nw / ow;
		float sh = (float)nh / oh;
		
		Matrix matrix = new Matrix();
		matrix.postScale(sw, sh);
		matrix.postRotate((float)orientation);
		
		return Bitmap.createBitmap(orig, 0, 0, ow, oh, matrix, true);
	}
	
	static Bitmap getResizedBitmapConstrained(Bitmap orig, int pixels, int orientation) {
		Axis a;
		
		if (orig.getWidth() <= pixels && orig.getHeight() <= pixels)
			return orig;
		
		if (orig.getWidth() > orig.getHeight())
			a = Axis.Horizontal;
		else
			a = Axis.Vertical;
		
		return getResizedBitmap(orig, pixels, a, orientation);
	}
}
