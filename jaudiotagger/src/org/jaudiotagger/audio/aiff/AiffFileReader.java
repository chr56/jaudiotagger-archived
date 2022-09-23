package org.jaudiotagger.audio.aiff;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.AudioFileReader2;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.tag.Tag;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Reads Audio and Metadata information contained in Aiff file.
 */
public class AiffFileReader extends AudioFileReader2
{
    @Override
    protected GenericAudioHeader getEncodingInfo(RandomAccessFile file) throws CannotReadException, IOException
    {
        return new AiffInfoReader(file.toString()).read(file);
    }

    @Override
    protected Tag getTag(RandomAccessFile file) throws CannotReadException, IOException
    {
        return new AiffTagReader(file.toString()).read(file);
    }
}
