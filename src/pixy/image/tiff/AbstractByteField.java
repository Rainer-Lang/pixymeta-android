/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package pixy.image.tiff;

import java.io.IOException;

import pixy.io.RandomAccessOutputStream;
import pixy.string.StringUtils;

public abstract class AbstractByteField extends TiffField<byte[]> {

	public AbstractByteField(short tag, FieldType fieldType, byte[] data) {
		super(tag, fieldType, data.length);
		this.data = data;
	}
	
	public String getDataAsString() {
		return StringUtils.byteArrayToHexString(data, 0, 10);
	}

	protected int writeData(RandomAccessOutputStream os, int toOffset) throws IOException {
	
		if (data.length <= 4) {
			dataOffset = (int)os.getStreamPointer();
			byte[] tmp = new byte[4];
			System.arraycopy(data, 0, tmp, 0, data.length);
			os.write(tmp);
		} else {
			dataOffset = toOffset;
			os.writeInt(toOffset);
			os.seek(toOffset);
			os.write(data);
			toOffset += data.length;
		}
		return toOffset;
	}
}