package com.example.s162132.childapp;

import android.content.Context;
import android.content.SharedPreferences;

import io.skyway.Peer.ConnectOption;
import io.skyway.Peer.DataConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerOption;

/**
 * Created by s162132 on 2017/07/02.
 */

public class SkyWayIdSetting {
    String id;
    Peer peer;
    SharedPreferences pref;
    private DataConnection dataConnection;
    private successfulGetPeerIdListener successfulGetPeerIdListener;

    public Peer getPeerId(Context context) {
        Context con = context;
        pref = con.getSharedPreferences("SkyWayId", Context.MODE_PRIVATE);
        String str = pref.getString("myId", null);
        System.out.println("getPeerId:str=" + str);

        PeerOption option = new PeerOption();
        option.key = "26fb276e-33c6-45fb-8ff6-aedd8a50af5f";
        option.domain = "localhost";
        option.debug = Peer.DebugLevelEnum.ALL_LOGS;

        if (str == null) {
            System.out.println("called:str == null");
            peer = new Peer(con, option);
            setPeerCallBack(peer);
        } else {
            System.out.println("called:str != null");
            peer = new Peer(con, str, option);
            setPeerCallBack(peer);
        }

        return peer;
    }

    public void setPeerCallBack(Peer peer) {
        peer.on(Peer.PeerEventEnum.OPEN, new OnCallback(){
            public void onCallback(Object object){
                if (object instanceof String){
                    id = (String) object;
                    System.out.println("PeerEventOPEN:peerId=" + id);

                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("myId", id);
                    editor.commit();
                    successfulGetPeerIdListener.successfulGetPeerId();
                }
            }
        });
    }

    public DataConnection connectStart(String parentId, Peer peer) {
        System.out.println("called:connectStart");

        //skyWayのoptionを設定する
        ConnectOption option = new ConnectOption();
        option.metadata = "data connection";
        option.label = "chat";
        option.serialization = DataConnection.SerializationEnum.BINARY;

        System.out.println("connectStart:parentId=" + parentId);
        //子どもと接続する
        dataConnection = peer.connect(parentId, option);
        System.out.println("connectStart:dataConnection=" + dataConnection);

        return dataConnection;
    }

    public interface successfulGetPeerIdListener {
        void successfulGetPeerId();
    }

    public void successfulGetPeerId(successfulGetPeerIdListener listener) {
        this.successfulGetPeerIdListener = listener;
    }
}
