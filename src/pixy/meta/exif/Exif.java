/**
 * Copyright (c) 2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * Exif.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    10Apr2015  Moved data loaded checking to ExifReader
 * WY    31Mar2015  Fixed bug with getImageIFD() etc
 * WY    13Mar2015  Initial creation
 */

package pixy.meta.exif;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import pixy.image.tiff.TIFFMeta;
import pixy.meta.Metadata;
import pixy.meta.MetadataType;
import pixy.meta.exif.ExifReader;
import pixy.meta.exif.ExifTag;
import pixy.meta.exif.ExifThumbnail;
import pixy.meta.exif.GPSTag;
import pixy.image.tiff.FieldType;
import pixy.image.tiff.IFD;
import pixy.image.tiff.TiffField;
import pixy.image.tiff.TiffTag;
import pixy.io.IOUtils;

/**
 * EXIF wrapper
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2014
 */
public abstract class Exif extends Metadata {
	protected IFD imageIFD;
	protected IFD exifSubIFD;
	protected IFD gpsSubIFD;
	protected ExifThumbnail thumbnail;
	
	public static final int FIRST_IFD_OFFSET = 0x08;
	
	private ExifReader reader;
	
	private boolean isThumbnailRequired;
	
	public Exif() {
		super(MetadataType.EXIF, null);
	}
	
	public Exif(byte[] data) {
		super(MetadataType.EXIF, data);
		this.reader = new ExifReader(data);
	}
	
	public Exif(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}
	
	public Exif(IFD imageIFD) {
		this();
		setImageIFD(imageIFD);
	}
	
	public void addExifField(ExifTag tag, FieldType type, Object data) {
		if(exifSubIFD == null)
			exifSubIFD = new IFD();
		TiffField<?> field = FieldType.createField(tag, type, data);
		if(field != null)
			exifSubIFD.addField(field);
		else
			throw new IllegalArgumentException("Cannot create required EXIF TIFF field");
	}
	
	public void addGPSField(GPSTag tag, FieldType type, Object data) {
		if(gpsSubIFD == null)
			gpsSubIFD = new IFD();
		TiffField<?> field = FieldType.createField(tag, type, data);
		if(field != null)
			gpsSubIFD.addField(field);
		else
			throw new IllegalArgumentException("Cannot create required GPS TIFF field");
	}
	
	public void addImageField(TiffTag tag, FieldType type, Object data) {
		if(imageIFD == null)
			imageIFD = new IFD();
		TiffField<?> field = FieldType.createField(tag, type, data);
		if(field != null)
			imageIFD.addField(field);
		else
			throw new IllegalArgumentException("Cannot create required Image TIFF field");
	}
	
	public boolean containsThumbnail() {
		if(thumbnail != null)
			return true;
		else
			return reader != null && reader.containsThumbnail();
	}
	
	public IFD getImageIFD() {
		if(imageIFD != null) {
			return new IFD(imageIFD);
		} else if(reader != null) {
			return reader.getImageIFD();
		}		
		return null;		
	}
	
	public IFD getExifIFD() {
		if(exifSubIFD != null) {
			return new IFD(exifSubIFD);
		} else if (reader != null) {
			return reader.getExifIFD();
		}		
		return null;
	}
	
	public IFD getGPSIFD() {
		if(gpsSubIFD != null) {
			return new IFD(gpsSubIFD);
		} else if (reader != null) {
			return reader.getGPSIFD();
		}			
		return null;		
	}
	
	public ExifReader getReader() {
		return reader;
	}
	
	public ExifThumbnail getThumbnail() {
		if(thumbnail != null)
			return thumbnail;
		if(reader != null) {
			return reader.getThumbnail();
		}		
		return null;
	}
	
	public boolean isThumbnailRequired() {
		return isThumbnailRequired;
	}
	
	public void setExifIFD(IFD exifSubIFD) {
		this.exifSubIFD = exifSubIFD;
	}
	
	public void setGPSIFD(IFD gpsSubIFD) {
		this.gpsSubIFD = gpsSubIFD;
	}
	
	public void setImageIFD(IFD imageIFD) {
		this.imageIFD = imageIFD;
		IFD exifSubIFD = imageIFD.getChild(TiffTag.EXIF_SUB_IFD);
		if(exifSubIFD != null)
			this.exifSubIFD = exifSubIFD;
		IFD gpsSubIFD = imageIFD.getChild(TiffTag.GPS_SUB_IFD);
		if(gpsSubIFD != null)
			this.gpsSubIFD = gpsSubIFD;
	}
	
	/**
	 * @param thumbnail a Thumbnail instance. If null, a thumbnail
	 *        will be generated from the input image.
	 */	
	public void setThumbnail(ExifThumbnail thumbnail) {
		this.thumbnail = thumbnail;
	}
	
	public void setThumbnailImage(Bitmap thumbnail) {
		if(this.thumbnail == null)
			this.thumbnail = new ExifThumbnail(); 
		this.thumbnail.setImage(thumbnail);
	}
	
	public void setThumbnailRequired(boolean isThumbnailRequired) {
		this.isThumbnailRequired = isThumbnailRequired;
	}
	
	@Override
	public void showMetadata() {
		if(imageIFD != null) {
			System.out.println("<<Image IFD starts>>");
			TIFFMeta.printIFD(imageIFD, TiffTag.class, "");
			System.out.println("<<Image IFD ends>>");
		} else
			super.showMetadata();
	}
	
	public abstract void write(OutputStream os) throws IOException;
}