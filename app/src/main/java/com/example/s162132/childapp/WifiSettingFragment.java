package com.example.s162132.childapp;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.WIFI_SERVICE;


/**
 * A simple {@link Fragment} subclass.
 */
public class WifiSettingFragment extends Fragment {

    private ListView listView;
    private ArrayList<String> arrayList;
    private Context context;
    private String str = "";

    public WifiSettingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_wifi_setting, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listView = (ListView) getActivity().findViewById(R.id.listView);
        context = getContext();
        final WifiManager manager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        if (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            List<ScanResult> results = manager.getScanResults();
            arrayList = new ArrayList<String>();
            String str;
            for (int i=0;i<results.size();++i) {
                str = results.get(i).SSID;
                if (!str.equals("")) {
                    arrayList.add(str);
                }
            }
            ArrayAdapter<String> adapter =
                    new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_multiple_choice, arrayList);
            listView.setAdapter(adapter);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        BootstrapButton registration = (BootstrapButton) getActivity().findViewById(R.id.registration);
        SharedPreferences pref = context.getSharedPreferences("SSID", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = pref.edit();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //listViewをタップしたときの処理
            }
        });

        registration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.clear();
                SparseBooleanArray checked = listView.getCheckedItemPositions();
                for (int ii = 0; ii < checked.size(); ii++) {
                    if (checked.valueAt(ii)) {
                        str += arrayList.get(checked.keyAt(ii)) + ",";
                    }
                }
                str = str.substring(0, str.length()-1);
                //Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
                editor.putString("SSID", str);
                editor.commit();
                str = "";
                Toast.makeText(getActivity(), "登録しました。", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
