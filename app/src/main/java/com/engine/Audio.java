package com.engine;

import android.media.MediaPlayer;
import android.content.res.AssetFileDescriptor;
import android.content.Context;
import java.util.List;
import java.util.ArrayList;

public class Audio {
    public static List<MediaPlayer> mps = new ArrayList<MediaPlayer>();

    public static MediaPlayer tocarMusica(Context ctx, String caminho) {
        return tocarMusica(ctx, caminho, true);
    }

    public static MediaPlayer tocarMusica(Context ctx, String caminho, boolean loop) {
        AssetFileDescriptor afd = null;
        try {
            afd = ctx.getAssets().openFd(caminho);
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mp.setLooping(loop);
            mp.prepare();
            mp.start();
            mps.add(mp);
            return mp;
        } catch(Exception e) {
            System.out.println("erro: " + e);
            return null;
        } finally {
            if(afd != null) {
                try {
                    afd.close();
                } catch(Exception e) {
                    System.out.println("erro: " + e);
                }
            }
        }
    }

    public static void pararMusicas() {
        for(int i = mps.size() - 1; i >= 0; i--) {
            MediaPlayer mp = mps.get(i);
            if(mp != null) {
                try {
                    mp.stop();
                } catch(Exception e) {
                    System.out.println("erro: " + e);
                }
                mp.release();
                mps.remove(i);
            }
        }
    }

    public static void pararMusica(MediaPlayer mp) {
        if(mp != null) {
            try {
                mp.stop();
            } catch(Exception e) {
                System.out.println("erro: " + e);
            }
            mp.release();
            mps.remove(mp);
        }
    }
}
