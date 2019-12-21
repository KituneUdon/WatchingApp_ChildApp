package com.example.s162132.childapp;

import io.skyway.Peer.Browser.MediaConstraints;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.Browser.Navigator;
import io.skyway.Peer.MediaConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;

import static io.skyway.Peer.Browser.MediaConstraints.CameraPositionEnum.BACK;

/**
 * Created by s162132 on 2017/07/08.
 */

public class VideoSetting {

    private MediaConnection mediaConnection;
    private MediaStream mediaStream;
    private Peer peer;

    public VideoSetting(Peer peer) {
        this.peer = peer;
    }

    public void getStream() {
        setPeerCallback();
    }

    public void setPeerCallback() {
        peer.on(Peer.PeerEventEnum.CALL, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                //相手から通話がかかってきた
                System.out.println("called:PeerEventEnum.CALL");
                System.out.println("Object = " + o);
                //映像の取得
                Navigator.initialize(peer);
                MediaConstraints constraints = new MediaConstraints();
                constraints.cameraPosition = BACK;
                mediaStream = Navigator.getUserMedia(constraints);
                mediaConnection = (MediaConnection) o;
                mediaConnection.answer(mediaStream);
                setMediaCallback();
            }
        });
    }

    public void setMediaCallback() {
        mediaConnection.on(MediaConnection.MediaEventEnum.CLOSE, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("called:MediaEventEnum.CLOSE");
                mediaConnection.close();
                mediaStream.close();
                Navigator.terminate();
                mediaStream = null;
                Navigator.terminate();
                unsetMediaCallback();
            }
        });

        mediaConnection.on(MediaConnection.MediaEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                //エラー処理
                System.out.println("エラーが発生しました。");
            }
        });
    }

    public void unsetMediaCallback() {
        mediaConnection.on(MediaConnection.MediaEventEnum.CLOSE, null);
        mediaConnection.on(MediaConnection.MediaEventEnum.ERROR, null);
    }
}
