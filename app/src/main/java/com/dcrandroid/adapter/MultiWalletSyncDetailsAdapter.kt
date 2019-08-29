/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.util.WalletData
import dcrlibwallet.HeadersFetchProgressReport
import dcrlibwallet.MultiWallet
import kotlinx.android.synthetic.main.multi_wallet_fetch_headers.view.*
import com.dcrandroid.R
import com.dcrandroid.util.Utils

class MultiWalletSyncDetailsAdapter(private val context: Context, private var openedWallets: List<Long>): RecyclerView.Adapter<MultiWalletSyncDetailsAdapter.ViewHolder>() {

    private val multiWallet: MultiWallet
    get() = WalletData.getInstance().multiWallet

    var fetchProgressReport: HeadersFetchProgressReport? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.multi_wallet_fetch_headers,
                parent, false))
    }

    override fun getItemCount(): Int {
        return openedWallets.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val wallet = multiWallet.getWallet(openedWallets[position])

        holder.walletName.text = wallet.walletName

        if(wallet.isWaiting){
            holder.walletStatus.let {
                it.setTextColor(context.resources.getColor(R.color.lightGray))
                it.setText(R.string.waiting_for_other_wallets)
            }

            if(fetchProgressReport != null){
                // ## of ######
                holder.fetchCount.text = context.getString(R.string.block_header_fetched_count,
                        wallet.bestBlock, fetchProgressReport!!.totalHeadersToFetch)

                // ## days behind
                val lastHeaderRelativeTime = (System.currentTimeMillis() / 1000) - wallet.bestBlockTimeStamp
                holder.daysBehind.text = Utils.getDaysBehind(lastHeaderRelativeTime, context)
            }

        }else{
            holder.walletStatus.let {
                it.setTextColor(context.resources.getColor(R.color.greenTextColor))
                it.setText(R.string.syncing_ellipsis)
            }

            if(fetchProgressReport != null){
                // ## of ######
                holder.fetchCount.text = context.getString(R.string.block_header_fetched_count,
                        fetchProgressReport!!.fetchedHeadersCount, fetchProgressReport!!.totalHeadersToFetch)

                // ## days behind
                val lastHeaderRelativeTime = (System.currentTimeMillis() / 1000) - fetchProgressReport!!.currentHeaderTimestamp
                holder.daysBehind.text = Utils.getDaysBehind(lastHeaderRelativeTime, context)
            }
        }


        if(fetchProgressReport == null){
            // ## of ######
            holder.fetchCount.text = "0"

            // ## days behind
            holder.daysBehind.text = null
        }
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val walletName = itemView.wallet_name
        val walletStatus = itemView.wallet_syncing_status

        val fetchCount = itemView.tv_fetch_discover_scan_count
        val daysBehind = itemView.tv_days
    }
}