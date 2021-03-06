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
 * IRBReader.java
 *
 * Who   Date       Description
 * ====  =========  ========================================================
 * WY    14Apr2015  Added getThumbnailResource()
 * WY    13Apr2015  Changed thumbnail from IRBThumbnail to ThumbnailResource
 */

package pixy.meta.adobe;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.meta.MetadataReader;
import pixy.meta.Thumbnail;
import pixy.meta.adobe.IRBThumbnail;
import pixy.meta.adobe.ImageResourceID;
import pixy.meta.adobe.JPEGQuality;
import pixy.meta.adobe.IPTC_NAA;
import pixy.meta.adobe.VersionInfo;
import pixy.meta.adobe._8BIM;
import pixy.io.IOUtils;
import pixy.util.ArrayUtils;

/**
 * Photoshop Image Resource Block reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2015
 */
public class IRBReader implements MetadataReader {
	private byte[] data;
	private boolean containsThumbnail;
	private ThumbnailResource thumbnail;
	private boolean loaded;
	Map<Short, _8BIM> _8bims = new HashMap<Short, _8BIM>();
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(IRBReader.class);
	
	public IRBReader(byte[] data) {
		this.data = data;
	}
	
	public boolean containsThumbnail() {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return containsThumbnail;
	}
	
	public Map<Short, _8BIM> get8BIM() {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableMap(_8bims);
	}
	
	public IRBThumbnail getThumbnail()  {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return thumbnail.getThumbnail();
	}
	
	public ThumbnailResource getThumbnailResource() {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return thumbnail;
	}
	
	public boolean isDataLoaded() {
		return loaded;
	}
	
	@Override
	public void read() throws IOException {
		int i = 0;
		while((i+4) < data.length) {
			String _8bim = new String(data, i, 4);
			i += 4;			
			if(_8bim.equals("8BIM")) {
				short id = IOUtils.readShortMM(data, i);
				i += 2;
				// Pascal string for name follows
				// First byte denotes string length -
				int nameLen = data[i++]&0xff;
				if((nameLen%2) == 0) nameLen++;
				String name = new String(data, i, nameLen).trim();
				i += nameLen;
				//
				int size = IOUtils.readIntMM(data, i);
				i += 4;
				
				ImageResourceID eId = ImageResourceID.fromShort(id); 
				
				switch(eId){
					case JPEG_QUALITY:
						_8bims.put(id, new JPEGQuality(name, ArrayUtils.subArray(data, i, size)));
						break;
					case VERSION_INFO:
						_8bims.put(id, new VersionInfo(name, ArrayUtils.subArray(data, i, size)));
						break;
					case IPTC_NAA:
						byte[] newData = ArrayUtils.subArray(data, i, size);
						_8BIM iptcBim = _8bims.get(id);
						if(iptcBim != null) {
							byte[] oldData = iptcBim.getData();
							_8bims.put(id, new IPTC_NAA(name, ArrayUtils.concat(oldData, newData)));
						} else
							_8bims.put(id, new IPTC_NAA(name, newData));
						break;
					case THUMBNAIL_RESOURCE_PS4:
					case THUMBNAIL_RESOURCE_PS5:
						containsThumbnail = true;
						int thumbnailFormat = IOUtils.readIntMM(data, i); //1 = kJpegRGB. Also supports kRawRGB (0).
						int width = IOUtils.readIntMM(data, i + 4);
						int height = IOUtils.readIntMM(data, i + 8);
						// Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
						int widthBytes = IOUtils.readIntMM(data, i + 12);
						// Total size = widthbytes * height * planes
						int totalSize = IOUtils.readIntMM(data, i + 16);
						// Size after compression. Used for consistency check.
						int sizeAfterCompression = IOUtils.readIntMM(data, i + 20);
						short bitsPerPixel = IOUtils.readShortMM(data, i + 24); // Bits per pixel. = 24
						short numOfPlanes = IOUtils.readShortMM(data, i + 26); // Number of planes. = 1
						byte[] thumbnailData = null;
						if(thumbnailFormat == Thumbnail.DATA_TYPE_KJpegRGB)
							thumbnailData = ArrayUtils.subArray(data, i + 28, sizeAfterCompression);
						else if(thumbnailFormat == Thumbnail.DATA_TYPE_KRawRGB)
							thumbnailData = ArrayUtils.subArray(data, i + 28, totalSize);
						// JFIF data in RGB format. For resource ID 1033 (0x0409) the data is in BGR format.
						thumbnail = new ThumbnailResource(eId, thumbnailFormat, width, height, widthBytes, totalSize, sizeAfterCompression, bitsPerPixel, numOfPlanes, thumbnailData);
						_8bims.put(id, thumbnail);
						break;
					default:
						_8bims.put(id, new _8BIM(id, name, size, ArrayUtils.subArray(data, i, size)));
				}				
				
				i += size;
				if(size%2 != 0) i++; // Skip padding byte
			}
		}
		loaded = true;
	}
	
	public void showMetadata() {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
		LOGGER.info("<<Adobe IRB information starts>>");
		for(_8BIM _8bim : _8bims.values()) {
			_8bim.print();
		}
		if(containsThumbnail) {
			LOGGER.info("{}", thumbnail.getResouceID());
			int thumbnailFormat = thumbnail.getDataType(); //1 = kJpegRGB. Also supports kRawRGB (0).
			switch (thumbnailFormat) {
				case IRBThumbnail.DATA_TYPE_KJpegRGB:
					LOGGER.info("Thumbnail format: KJpegRGB");
					break;
				case IRBThumbnail.DATA_TYPE_KRawRGB:
					LOGGER.info("Thumbnail format: KRawRGB");
					break;
			}
			LOGGER.info("Thumbnail width: {}", thumbnail.getWidth());
			LOGGER.info("Thumbnail height: {}", thumbnail.getHeight());
			// Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
			LOGGER.info("Padded row bytes: {}", thumbnail.getPaddedRowBytes());
			// Total size = widthbytes * height * planes
			LOGGER.info("Total size: {}", thumbnail.getTotalSize());
			// Size after compression. Used for consistency check.
			LOGGER.info("Size after compression: {}", thumbnail.getCompressedSize());
			// Bits per pixel. = 24
			LOGGER.info("Bits per pixel: {}", thumbnail.getBitsPerPixel());
			// Number of planes. = 1
			LOGGER.info("Number of planes: {}", thumbnail.getNumOfPlanes());
		}
		
		LOGGER.info("<<Adobe IRB information ends>>");
	}
}