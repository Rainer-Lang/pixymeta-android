/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 * 
 * Change History - most recent changes go on top of previous changes
 *
 * ThumbnailResource.java
 * <p>
 * Adobe thumbnail resource wrapper
 *
 * Who   Date       Description
 * ====  =========  ==================================================
 * WY    14Apr2015  Added new constructor
 * WY    13Apr2015  Initial creation
 */

package pixy.meta.adobe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.graphics.Bitmap;
import pixy.io.IOUtils;
import pixy.meta.Thumbnail;
import pixy.util.MetadataUtils;

public class ThumbnailResource extends _8BIM {
	private int width;
	private int height;
	//Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
	private int paddedRowBytes;
	// Total size = widthbytes * height * planes
	private int totalSize;
	// Size after compression. Used for consistency check.
	private int compressedSize;
	// Bits per pixel. = 24
	private int bitsPerPixel;
	// Number of planes. = 1
	private int numOfPlanes;
	private ImageResourceID id;
	private int dataType;
	// Thumbnail
	private IRBThumbnail thumbnail = new IRBThumbnail();
	
	public ThumbnailResource(Bitmap thumbnail) {
		this("THUMBNAIL_RESOURCE", thumbnail);
	}
		
	public ThumbnailResource(String name, Bitmap thumbnail) {
		super(ImageResourceID.THUMBNAIL_RESOURCE_PS5, name, null);
		try {
			this.thumbnail = createThumbnail(thumbnail);
		} catch (IOException e) {
			throw new RuntimeException("Unable to create IRBThumbnail from Bitmap");
		}
	}
	
	// id is either ImageResourceID.THUMBNAIL_RESOURCE_PS4 or ImageResourceID.THUMBNAIL_RESOURCE_PS5
	public ThumbnailResource(ImageResourceID id, int dataType, int width, int height, byte[] data) {
		// paddedRowBytes = (width * bits per pixel + 31) / 32 * 4.
		// totalSize = paddedRowBytes * height * planes
		// bitsPerPixel = 24
		// numOfPlanes = 1
		this(id, dataType, width, height, (width*24 + 31)/32*4, (width*24 + 31)/32*4*height*1, data.length, 24, 1, data);
	}
	
	// id is either ImageResourceID.THUMBNAIL_RESOURCE_PS4 or ImageResourceID.THUMBNAIL_RESOURCE_PS5
	public ThumbnailResource(ImageResourceID id, int dataType, int width, int height, int paddedRowBytes, int totalSize, int compressedSize, int bitsPerPixel, int numOfPlanes, byte[] data) {
		super(id, "HUMBNAIL_RESOURCE", data);
		// Sanity check
		if(id != ImageResourceID.THUMBNAIL_RESOURCE_PS4 && id != ImageResourceID.THUMBNAIL_RESOURCE_PS5)
			throw new IllegalArgumentException("Unsupported thumbnail ImageResourceID: " + id);
		// Initialize fields
		this.id = id;
		this.width = width;
		this.height = height;
		this.paddedRowBytes = paddedRowBytes;
		this.totalSize = totalSize;
		this.compressedSize = compressedSize;
		this.bitsPerPixel = bitsPerPixel;
		this.numOfPlanes = numOfPlanes;
		this.dataType = dataType;
		// JFIF data in RGB format. For resource ID 1033 (0x0409) the data is in BGR format.
		if(dataType == Thumbnail.DATA_TYPE_KJpegRGB) {
			thumbnail.setImage(width, height, dataType, data);
		} else if(dataType == Thumbnail.DATA_TYPE_KRawRGB) {
			// kRawRGB - NOT tested yet!
			int[] colors = null;
			if(id == ImageResourceID.THUMBNAIL_RESOURCE_PS4)
				colors = MetadataUtils.bgr2ARGB(data);
			else if(id == ImageResourceID.THUMBNAIL_RESOURCE_PS5)
				colors = MetadataUtils.toARGB(data);
			thumbnail.setImage(Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888));
		} else
			throw new UnsupportedOperationException("Unsupported IRB thumbnail data type: " + dataType);
	}
	
	private IRBThumbnail createThumbnail(Bitmap thumbnail) throws IOException {
		// Create memory buffer to write data
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		// Compress the thumbnail
		try {
			thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bout);
		} catch (Exception e) {
			e.printStackTrace();
		}
		byte[] data = bout.toByteArray();
		this.id = ImageResourceID.THUMBNAIL_RESOURCE_PS5;
		// Write thumbnail dimension
		this.width = thumbnail.getWidth();
		this.height = thumbnail.getHeight();
		// Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
		this.bitsPerPixel = 24;
		this.numOfPlanes = 1;
		this.paddedRowBytes = (width*bitsPerPixel + 31)/32*4;
		// Total size = widthbytes * height * planes
		this.totalSize = paddedRowBytes*height*numOfPlanes;
		// Size after compression. Used for consistency check.
		this.compressedSize = data.length;
		this.dataType = Thumbnail.DATA_TYPE_KJpegRGB;
			
		return new IRBThumbnail(width, height, dataType, data);
	}
	
	public int getBitsPerPixel() {
		return bitsPerPixel;
	}
	
	public int getCompressedSize() {
		return compressedSize;
	}
	
	public int getDataType() {
		return dataType;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getNumOfPlanes() {
		return numOfPlanes;
	}
	
	public int getPaddedRowBytes() {
		return paddedRowBytes;
	}
	
	public ImageResourceID getResouceID() {
		return id;
	}
	
	public IRBThumbnail getThumbnail() {
		return thumbnail;
	}
	
	public int getTotalSize() {
		return totalSize;		
	}
	
	public int getWidth() {
		return width;
	}
		
	public void write(OutputStream os) throws IOException {
		if(data == null) {			
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			thumbnail.write(bout);
			byte[] compressedData = bout.toByteArray();
			bout.reset();
			// Write thumbnail format
			IOUtils.writeIntMM(bout, dataType);
			IOUtils.writeIntMM(bout, width);
			IOUtils.writeIntMM(bout, height);
			IOUtils.writeIntMM(bout, paddedRowBytes);
			// Total size = widthbytes * height * planes
			IOUtils.writeIntMM(bout, totalSize);
			// Size after compression. Used for consistency check.
			IOUtils.writeIntMM(bout, compressedData.length);
			IOUtils.writeShortMM(bout, bitsPerPixel);
			IOUtils.writeShortMM(bout, numOfPlanes);
			bout.write(compressedData);
			data = bout.toByteArray();
			size = data.length;
		}
		super.write(os);
	}	
}