/*
 IRKitEndingFragment.java
 Copyright (c) 2014 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.irkit.settings.fragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.deviceconnect.android.deviceplugin.irkit.IRKitApplication;
import org.deviceconnect.android.deviceplugin.irkit.IRKitDevice;
import org.deviceconnect.android.deviceplugin.irkit.IRKitManager;
import org.deviceconnect.android.deviceplugin.irkit.R;
import org.deviceconnect.android.deviceplugin.irkit.data.IRKitDBHelper;
import org.deviceconnect.android.deviceplugin.irkit.data.VirtualProfileData;
import org.deviceconnect.android.deviceplugin.irkit.settings.activity.IRKitDeviceListActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Virtual Profileに赤外線を登録する.
 * @author NTT DOCOMO, INC.
 */
public class IRKitRegisterIRFragment extends Fragment  {

    /** Virtual Profile. */
    private VirtualProfileData mProfile;
    /** インジケーター. */
    private ProgressDialog mIndView;
    /** IRKit のデバイス.*/
    private IRKitDevice mDevice;
    /** DB helper. */
    private IRKitDBHelper mDBHelper;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        setRetainInstance(true);

        mDBHelper = new IRKitDBHelper(getActivity());
        View rootView = inflater.inflate(R.layout.fragment_register_ir, null);
        TextView titleView = (TextView) rootView.findViewById(R.id.text_view_number);
        titleView.setText(getString(R.string.edit_profile, mProfile.getProfile()));
        TextView apiView = (TextView) rootView.findViewById(R.id.api_name);
        apiView.setText(mProfile.getName());
        final Button registerIR = (Button) rootView.findViewById(R.id.register_ir);
        registerIR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgress();
                List<IRKitDevice> devices = getIRKitDevices();
                mDevice = null;
                if (devices != null) {
                    for (IRKitDevice d : devices) {
                        if (mProfile.getServiceId().indexOf(d.getName()) != -1) {
                            mDevice = d;
                            registerIR.getHandler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    IRKitManager.INSTANCE.fetchMessage(mDevice.getIp(),
                                            new IRKitManager.GetMessageCallback() {
                                                @Override
                                                public void onGetMessage(final String message) {
                                                    if (getActivity() == null) {
                                                        return;
                                                    }

                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (message == null) {
                                                                showFailureDialog();
                                                            } else if (!checkData(message)) {
                                                                showFailureDialog();
                                                            } else {
                                                                mProfile.setIr(message);
                                                                int i = mDBHelper.updateVirtualProfile(mProfile);
                                                                showSuccessDialog();
                                                            }
                                                            dismissProgress();
                                                        }
                                                    });
                                                }
                                            });
                                }
                            }, 5000);
                            break;
                        }
                    }
                }
                if (mDevice == null) {
                    showFailureDialog();
                    dismissProgress();
                    return;
                }

            }
        });
        return rootView;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final MenuItem menuItem = menu.add(getString(R.string.menu_close));
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                if (item.getTitle().equals(menuItem.getTitle())) {
                    getActivity().finish();
                }
                return true;
            }
        });
    }

    /**
     * Virtual Profile を設定する。
     * @param profile virtual Profile
     */
    public void setProfile(final VirtualProfileData profile) {
        mProfile = profile;
    }

    /**
     * IRKitデバイスリストの取得
     * @return IRKitデバイスリスト
     */
    private List<IRKitDevice> getIRKitDevices() {
        IRKitDeviceListActivity activity =
                (IRKitDeviceListActivity) getActivity();
        IRKitApplication application =
                (IRKitApplication) activity.getApplication();
        return application.getIRKitDevices();
    }

    /**
     * 赤外線照射中のダイアログを出す。
     */
    private void showProgress() {
        mIndView = new ProgressDialog(getActivity());
        mIndView.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mIndView.setCancelable(false);
        mIndView.setMessage(getString(R.string.receiving));
        mIndView.show();
    }

    /**
     * Progress を消す。
     */
    private void dismissProgress() {
        if (mIndView != null) {
            mIndView.dismiss();
        }
    }

    /**
     * 赤外線の登録失成功通知ダイアログ.
     *
     */
    private void showSuccessDialog() {
        IRKitCreateVirtualDeviceDialogFragment.showAlert(getActivity(),
                getString(R.string.receive_success_title),
                getString(R.string.receive_success_message));
    }

    /**
     * 赤外線の登録失敗通知ダイアログ.
     *
     */
    private void showFailureDialog() {
        IRKitCreateVirtualDeviceDialogFragment.showAlert(getActivity(),
                getString(R.string.receive_failure_title),
                getString(R.string.receive_failure_message));
    }

    /**
     * 送られてきたデータがIRKitに対応しているかチェックを行う.
     * @param message データ
     * @return フォーマットが問題ない場合はtrue、それ以外はfalse
     */
    private boolean checkData(final String message) {
        if (message == null || message.length() == 0) {
            return false;
        }
        try {
            JSONObject json = new JSONObject(message);
            String format = json.getString("format");
            int freq = json.getInt("freq");
            JSONArray datas = json.getJSONArray("data");
            return (format != null && freq > 0 && datas != null);
        } catch (JSONException e) {
            return false;
        }
    }
}
