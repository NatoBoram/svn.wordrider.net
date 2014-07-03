/*  
 * Copyright 2008 CoreMedia AG, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the License); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an AS IS BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */

package com.googlecode.mp4parser;


import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.FullBox;
import com.googlecode.mp4parser.annotations.DoNotParseDetail;

import java.nio.ByteBuffer;

/**
 * Base class for all ISO Full boxes.
 */
public abstract class AbstractFullBox extends AbstractBox implements FullBox {
    private int version;
    private int flags;

    protected AbstractFullBox(String type) {
        super(type);
    }

    protected AbstractFullBox(String type, byte[] userType) {
        super(type, userType);
    }

    @DoNotParseDetail
    public int getVersion() {
        // it's faster than the join point
        if (!isParsed) {
            parseDetails();
        }
        return version;
    }

    public void setVersion(int version) {
        if (!isParsed()) {
            parseDetails();
        }
        this.version = version;
    }

    @DoNotParseDetail
    public int getFlags() {
        // it's faster than the join point
        if (!isParsed) {
            parseDetails();
        }
        return flags;
    }

    public void setFlags(int flags) {
        if (!isParsed()) {
            parseDetails();
        }
        this.flags = flags;
    }


    /**
     * Parses the version/flags header and returns the remaining box size.
     *
     * @param content the <code>ByteBuffer</code> that contains the version &amp; flag
     * @return number of bytes read
     */
    protected final long parseVersionAndFlags(ByteBuffer content) {
        version = IsoTypeReader.readUInt8(content);
        flags = IsoTypeReader.readUInt24(content);
        return 4;
    }

    protected final void writeVersionAndFlags(ByteBuffer bb) {
        IsoTypeWriter.writeUInt8(bb, version);
        IsoTypeWriter.writeUInt24(bb, flags);
    }
}