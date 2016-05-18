package com.leepood.lark.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by leepood on 6/18/15.
 */
public class ToastManager {

    public static void show(Context ctx,CharSequence str){
        Toast.makeText(ctx,str,Toast.LENGTH_SHORT).show();
    }


}
