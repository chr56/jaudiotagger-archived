/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jaudiotagger.audio.aiff;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.aiff.chunk.ChunkType;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.generic.TagWriter;
import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.audio.iff.Chunk;
import org.jaudiotagger.audio.iff.ChunkHeader;
import org.jaudiotagger.audio.iff.IffHeaderChunk;
import org.jaudiotagger.logging.Hex;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.aiff.AiffTag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;
import static org.jaudiotagger.audio.iff.IffHeaderChunk.*;


/**
 * Write Aiff Tag.
 */
public class AiffTagWriter implements TagWriter
{
    // Logger Object
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.aiff");

    /**
     * Read existing metadata
     *
     * @param raf
     * @return tags within Tag wrapper
     * @throws IOException
     * @throws CannotWriteException
     */
    private AiffTag getExistingMetadata(RandomAccessFile raf) throws IOException, CannotWriteException
    {
        try
        {
            //Find AiffTag (if any)
            AiffTagReader im = new AiffTagReader();
            return im.read(raf);
        }
        catch (CannotReadException ex)
        {
            throw new CannotWriteException("Failed to read file");
        }
    }

    /**
     * Seek in file to start of LIST Metadata chunk
     *
     * @param raf
     * @param existingTag
     * @throws IOException
     * @throws CannotWriteException
     */
    private ChunkHeader seekToStartOfMetadata(RandomAccessFile raf, AiffTag existingTag) throws IOException, CannotWriteException
    {
        raf.seek(existingTag.getStartLocationInFileOfId3Chunk());
        final ChunkHeader chunkHeader = new ChunkHeader(ByteOrder.BIG_ENDIAN);
        chunkHeader.readHeader(raf);
        raf.seek(raf.getFilePointer() - ChunkHeader.CHUNK_HEADER_SIZE);

        if(!ChunkType.TAG.getCode().equals(chunkHeader.getID()))
        {
            throw new CannotWriteException("Unable to find ID3 chunk at expected location");
        }
        return chunkHeader;
    }

    /**
     *
     * @param existingTag
     * @param raf
     * @return true if at end of file (all take account padding byte)
     * @throws IOException
     */
    private boolean isAtEndOfFileAllowingForPaddingByte(AiffTag existingTag, RandomAccessFile raf) throws IOException
    {
        return ((existingTag.getID3Tag().getEndLocationInFile() == raf.length())||
                (((existingTag.getID3Tag().getEndLocationInFile() & 1) != 0) && existingTag.getID3Tag().getEndLocationInFile() + 1 == raf.length()));
    }
    /**
     * Delete given {@link Tag} from file.
     *
     * @param tag tag, must be instance of {@link AiffTag}
     * @param raf random access file
     * @param tempRaf temporary random access file
     * @throws java.io.IOException
     * @throws org.jaudiotagger.audio.exceptions.CannotWriteException
     */
    public void delete(final Tag tag, final RandomAccessFile raf, final RandomAccessFile tempRaf) throws IOException, CannotWriteException
    {
        logger.severe("Deleting tag from file");
        final AiffTag existingTag = getExistingMetadata(raf);

        if (existingTag.isExistingId3Tag() && existingTag.getID3Tag().getStartLocationInFile() != null)
        {
            ChunkHeader chunkHeader = seekToStartOfMetadata(raf, existingTag);
            if(isAtEndOfFileAllowingForPaddingByte(existingTag, raf))
            {
                logger.severe("Setting new length to:" + (existingTag.getStartLocationInFileOfId3Chunk()));
                raf.setLength(existingTag.getStartLocationInFileOfId3Chunk());
            }
            else
            {
                logger.severe("Deleting tag chunk");
                deleteTagChunk(raf, existingTag, chunkHeader);
            }
            rewriteRiffHeaderSize(raf);
        }
        logger.severe("Deleted tag from file");

    }

    /**
     * <p>Deletes the given ID3-{@link Tag}/{@link Chunk} from the file by moving all following chunks up.</p>
     * <pre>
     * [chunk][-id3-][chunk][chunk]
     * [chunk] &lt;&lt;--- [chunk][chunk]
     * [chunk][chunk][chunk]
     * </pre>
     *
     * @param raf random access file
     * @param existingTag existing tag
     * @param tagChunkHeader existing chunk header for the tag
     * @throws IOException if something goes wrong
     */
    private void deleteTagChunk(final RandomAccessFile raf, final AiffTag existingTag, final ChunkHeader tagChunkHeader) throws IOException
    {
        int lengthTagChunk = (int) tagChunkHeader.getSize() + ChunkHeader.CHUNK_HEADER_SIZE;
        if ((lengthTagChunk & 1) != 0)
        {
            lengthTagChunk++;
        }
        final long newLength = raf.length() - lengthTagChunk;
        logger.severe("Size of id3 chunk to delete is:"+newLength);

        // position for reading after the id3 tag
        raf.seek(existingTag.getStartLocationInFileOfId3Chunk() + lengthTagChunk );
        final FileChannel channel = raf.getChannel();

        deleteTagChunkUsingSmallByteBufferSegments(existingTag, channel, newLength, lengthTagChunk);
        // truncate the file after the last chunk
        logger.config("Setting new length to:" + newLength);
        raf.setLength(newLength);
    }

    /**
     * The following seems to work on Windows but hangs on OSX!
     * Bug is filed <a href="https://bugs.openjdk.java.net/browse/JDK-8140241">here</a>.
     *
     * @param existingTag existing tag
     * @param channel channel
     * @param newLength new length
     * @throws IOException if something goes wrong
     */
    private void deleteTagChunkUsingChannelTransfer(final AiffTag existingTag, final FileChannel channel, final long newLength)
            throws IOException
    {
        long read;
        //Read from just after the ID3Chunk into the channel at where the ID3 chunk started, should usually only require one transfer
        //but put into loop in case multiple calls are required
        for (long position = existingTag.getStartLocationInFileOfId3Chunk();
             (read = channel.transferFrom(channel, position, newLength - position)) < newLength-position;
             position += read);//is this problem if loop called more than once do we need to update position of channel to modify
        //where write to ?
    }

    /**
     * Use ByteBuffers to copy a 4mb chunk, write the chunk and repeat until the rest of the file after the ID3 tag
     * is rewritten
     *
     * @param existingTag existing tag
     * @param channel channel
     * @param newLength new length
     * @param lengthTagChunk length tag chunk
     * @throws IOException if something goes wrong
     */
    // TODO: arguments are not used, position is implicit
    private void deleteTagChunkUsingSmallByteBufferSegments(final AiffTag existingTag, final FileChannel channel, final long newLength, final long lengthTagChunk)
            throws IOException
    {
        final ByteBuffer buffer = ByteBuffer.allocateDirect((int)TagOptionSingleton.getInstance().getWriteChunkSize());
        while (channel.read(buffer) >= 0 || buffer.position() != 0) {
            buffer.flip();
            final long readPosition = channel.position();
            channel.position(readPosition-lengthTagChunk-buffer.limit());
            channel.write(buffer);
            channel.position(readPosition);
            buffer.compact();
        }
    }

    /**
     * Write {@link Tag} to file.
     *
     * @param af audio file
     * @param tag tag, must be instance of {@link AiffTag}
     * @param raf random access file
     * @param rafTemp temporary random access file
     * @throws org.jaudiotagger.audio.exceptions.CannotWriteException
     * @throws java.io.IOException
     */
    public void write(final AudioFile af, final Tag tag, final RandomAccessFile raf, final RandomAccessFile rafTemp) throws CannotWriteException, IOException
    {
        logger.severe("Writing Aiff tag to file");
        final AiffTag existingTag = getExistingMetadata(raf);
        try
        {
            final AiffTag     aiffTag     = (AiffTag) tag;
            final ByteBuffer  bb          = convert(aiffTag, existingTag);
            final long        newTagSize  = bb.limit();

            //Replacing ID3 tag
            if (existingTag.isExistingId3Tag() && existingTag.getID3Tag().getStartLocationInFile() != null)
            {
                final ChunkHeader chunkHeader = seekToStartOfMetadata(raf, existingTag);
                logger.info("Current Space allocated:" + existingTag.getSizeOfID3TagOnly() + ":NewTagRequires:" + newTagSize);

                //Usual case ID3 is last chunk
                if(isAtEndOfFileAllowingForPaddingByte(existingTag, raf))
                {
                    writeDataToFile(raf, bb, newTagSize);
                }
                //Unusual Case where ID3 is not last chunk
                else
                {
                    deleteTagChunk(raf, existingTag, chunkHeader);
                    //Go to end of file
                    raf.seek(raf.length());
                    writeDataToFile(raf, bb, newTagSize);
                }
            }
            //New Tag
            else
            {
                //Go to end of file
                raf.seek(raf.length());
                writeDataToFile(raf, bb, newTagSize);
            }
            rewriteRiffHeaderSize(raf);
        }
        finally
        {
            raf.close();
        }
    }


    /**
     * Rewrite RAF header to reflect new file file
     *
     * @param raf
     * @throws IOException
     */
    private void rewriteRiffHeaderSize(RandomAccessFile raf) throws IOException {
        raf.seek(IffHeaderChunk.SIGNATURE_LENGTH);
        raf.write(Utils.getSizeBEInt32(((int) raf.length()) - SIGNATURE_LENGTH - SIZE_LENGTH));
    }

    /**
     * Writes data as a {@link ChunkType#TAG} chunk to the file.
     *
     * @param raf random access file
     * @param bb data to write
     * @param chunkSize chunk size
     * @throws IOException
     */
    private void writeDataToFile(final RandomAccessFile raf,  final ByteBuffer bb, final long chunkSize)
            throws IOException
    {
        final ChunkHeader ch = new ChunkHeader(ByteOrder.BIG_ENDIAN);
        ch.setID(ChunkType.TAG.getCode());
        ch.setSize(chunkSize);
        raf.getChannel().write(ch.writeHeader());
        raf.getChannel().write(bb);
        writeExtraByteIfChunkOddSize(raf, chunkSize );
    }

    /**
     * Chunk must also start on an even byte so if our chunksize is odd we need
     * to write another byte. But this should never happen as ID3Tag is now amended
     * to ensure always write padding byte if needed to stop it being odd sized.
     *
     * @param raf
     * @param size
     * @throws IOException
     */
    private void writeExtraByteIfChunkOddSize(RandomAccessFile raf, long size )
            throws IOException
    {
        if ((size & 1) != 0)
        {
            raf.write(new byte[1]);
        }
    }

    /**
     * Converts tag to {@link ByteBuffer}.
     *
     * @param tag tag
     * @param existingTag
     * @return byte buffer containing the tag data
     * @throws UnsupportedEncodingException
     */
    public ByteBuffer convert(final AiffTag tag, AiffTag existingTag) throws UnsupportedEncodingException
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            long existingTagSize = existingTag.getSizeOfID3TagOnly();

            //If existingTag is uneven size lets make it even
            if( existingTagSize > 0)
            {
                if((existingTagSize & 1)!=0)
                {
                    existingTagSize++;
                }
            }

            //Write Tag to buffer
            tag.getID3Tag().write(baos, (int)existingTagSize);

            //If the tag is now odd because we needed to increase size and the data made it odd sized
            //we redo adding a padding byte to make it even
            if((baos.toByteArray().length & 1)!=0)
            {
                int newSize = baos.toByteArray().length + 1;
                baos = new ByteArrayOutputStream();
                tag.getID3Tag().write(baos, newSize);
            }
            final ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());
            buf.rewind();
            return buf;
        }
        catch (IOException ioe)
        {
            //Should never happen as not writing to file at this point
            throw new RuntimeException(ioe);
        }
    }
}

