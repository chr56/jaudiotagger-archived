package org.jaudiotagger.tag.id3.framebody;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.id3.valuepair.TextEncoding;

import java.io.File;


public class FrameBodyTMCLTest extends AbstractTestCase
{
    /**
     * Uses TMCL frame
     * @throws Exception
     */
    public void testWritePERFORMERsIDv24() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePERFORMERsv24.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v24Tag());
        f.getTag().setField(FieldKey.PERFORMER,"violinist","Nigel Kennedy");
        f.getTag().addField(FieldKey.PERFORMER,"harpist","Gloria Divosky");
        assertEquals(1, f.getTag().getFieldCount());
        assertEquals("violinist:Nigel Kennedy", f.getTag().getFirst(FieldKey.PERFORMER));
        assertEquals("violinist:Nigel Kennedy", f.getTag().getValue(FieldKey.PERFORMER,0));
        assertEquals("harpist:Gloria Divosky", f.getTag().getValue(FieldKey.PERFORMER,1));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.PERFORMER).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());
    }

    public void testWritePERFORMERsAndDeleteIDv24() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePERFORMERsAndDeletev24.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v24Tag());
        f.getTag().setField(FieldKey.PERFORMER,"violinist","Nigel Kennedy");
        f.getTag().addField(FieldKey.PERFORMER,"harpist","Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.PERFORMER).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());

        f.getTag().deleteField(FieldKey.PERFORMER);
        assertEquals(0,f.getTag().getFieldCount());
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(0,f.getTag().getFields(FieldKey.PERFORMER).size());
        assertEquals(0,f.getTag().getFieldCount());
        assertEquals(0, f.getTag().getFieldCount());

    }

    public void testWritePERFORMERsIDv23() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePERFORMERsv23.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v23Tag());
        f.getTag().setField(FieldKey.PERFORMER,"violinist","Nigel Kennedy");
        f.getTag().addField(FieldKey.PERFORMER,"harpist","Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals("violinist:Nigel Kennedy", f.getTag().getFirst(FieldKey.PERFORMER));
        assertEquals("violinist:Nigel Kennedy", f.getTag().getValue(FieldKey.PERFORMER,0));
        assertEquals("harpist:Gloria Divosky", f.getTag().getValue(FieldKey.PERFORMER,1));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.PERFORMER).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());
    }

    public void testWritePERFORMERsIDv22() throws Exception
    {
        File testFile = AbstractTestCase.copyAudioToTmp("testV1.mp3", new File("testWritePERFORMERsv22.mp3"));
        AudioFile f = AudioFileIO.read(testFile);
        assertNull(f.getTag());

        f.setTag(new ID3v22Tag());
        f.getTag().setField(FieldKey.PERFORMER,"violinist","Nigel Kennedy");
        f.getTag().addField(FieldKey.PERFORMER,"harpist","Gloria Divosky");
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals("violinist:Nigel Kennedy", f.getTag().getFirst(FieldKey.PERFORMER));
        assertEquals("violinist:Nigel Kennedy", f.getTag().getValue(FieldKey.PERFORMER,0));
        assertEquals("harpist:Gloria Divosky", f.getTag().getValue(FieldKey.PERFORMER,1));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals(1,f.getTag().getFields(FieldKey.PERFORMER).size());
        assertEquals(1,f.getTag().getFieldCount());
        assertEquals(1, f.getTag().getFieldCount());
    }

}