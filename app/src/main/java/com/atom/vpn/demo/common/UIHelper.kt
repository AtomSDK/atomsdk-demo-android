package com.atom.vpn.demo.common

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper

/**
 * Static UI helpers. Members are annotated [JvmStatic] so the remaining Java sources can keep
 * calling them unqualified; those annotations can be dropped once the migration is complete.
 */
object UIHelper {

    @JvmStatic
    fun showAlertDialog(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(message)
                .setTitle("Alert")
                .setCancelable(true)
                .setNegativeButton("OK") { dialog, _ -> dialog.cancel() }

            val alert = builder.create()
            alert.show()
        }
    }

    @JvmStatic
    fun showListDialogBox(
        mContext: Context,
        mTitle: String,
        array: Array<String>,
        onClickListener: DialogInterface.OnClickListener
    ) {
        val dialog = AlertDialog.Builder(mContext)
            .setTitle(mTitle)
            .setItems(array) { dialog, which ->
                onClickListener.onClick(dialog, which)
                dialog.dismiss()
            }
            .setPositiveButton("OK", onClickListener)
            .setNegativeButton("Cancel") { _, _ ->
                //  Your code when user clicked on Cancel
            }
            .create()

        dialog.show()
    }
}