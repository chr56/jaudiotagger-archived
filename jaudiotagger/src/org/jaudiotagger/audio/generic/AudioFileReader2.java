package org.jaudiotagger.audio.generic;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.NoReadPermissionsException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.logging.ErrorMessage;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;

/**
 * Replacement for AudioFileReader class
 */
public abstract class AudioFileReader2 extends AudioFileReader
{
    /*
   * Reads the given file, and return an AudioFile object containing the Tag
   * and the encoding infos present in the file. If the file has no tag, an
   * empty one is returned. If the encodinginfo is not valid , an exception is thrown.
   *
   * @param f The file to read
   * @exception NoReadPermissionsException if permissions prevent reading of file
   * @exception CannotReadException If anything went bad during the read of this file
   */
    public AudioFile read(File f) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.config(ErrorMessage.GENERAL_READ.getMsg(f.getAbsolutePath()));
        }

        if (!f.canRead())
        {
            if(!f.exists())
            {
                throw new FileNotFoundException(ErrorMessage.UNABLE_TO_FIND_FILE.getMsg(f.getAbsolutePath()));
            }
            else
            {
                throw new NoReadPermissionsException(ErrorMessage.GENERAL_READ_FAILED_DO_NOT_HAVE_PERMISSION_TO_READ_FILE.getMsg(f.getAbsolutePath()));
            }
        }

        if (f.length() <= MINIMUM_SIZE_FOR_VALID_AUDIO_FILE)
        {
            throw new CannotReadException(ErrorMessage.GENERAL_READ_FAILED_FILE_TOO_SMALL.getMsg(f.getAbsolutePath()));
        }

        RandomAccessFile raf = new RandomAccessFile(f,"rw");
        GenericAudioHeader info = getEncodingInfo(raf);
        Tag tag = getTag(raf);
        return new AudioFile(f, info, tag);
    }

    protected abstract GenericAudioHeader getEncodingInfo(RandomAccessFile raf) throws CannotReadException, IOException;

    /**
     * Read tag Information
     *
     * @param file
     * @return
     * @throws CannotReadException
     * @throws IOException
     */
    protected abstract Tag getTag(RandomAccessFile file) throws CannotReadException, IOException;
}
