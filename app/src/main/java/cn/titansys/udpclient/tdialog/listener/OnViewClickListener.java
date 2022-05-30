package cn.titansys.udpclient.tdialog.listener;

import android.view.View;

import cn.titansys.udpclient.tdialog.TDialog;
import cn.titansys.udpclient.tdialog.base.BindViewHolder;


public interface OnViewClickListener {
    void onViewClick(BindViewHolder viewHolder, View view, TDialog tDialog);
}
