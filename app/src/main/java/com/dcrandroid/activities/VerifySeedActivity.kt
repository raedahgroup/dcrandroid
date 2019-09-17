/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.adapter.*
import com.dcrandroid.data.Constants
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import dcrlibwallet.LibWallet
import dcrlibwallet.MultiWallet
import kotlinx.android.synthetic.main.verify_seed_page.*
import java.lang.Exception

class VerifySeedActivity : BaseActivity() {

    private lateinit var seeds: Array<String>
    private lateinit var allSeeds: Array<String>

    private lateinit var verifySeedAdapter: VerifySeedAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    private var wallet: LibWallet? = null
    private var multiWallet: MultiWallet = WalletData.multiWallet!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(R.layout.verify_seed_page)

        linearLayoutManager = LinearLayoutManager(this)
        recycler_view_seeds.layoutManager = linearLayoutManager

        allSeeds = Utils.getWordList(this).split(" ").toTypedArray()
        prepareData()

        recycler_view_seeds.viewTreeObserver.addOnScrollChangedListener{

            val firstVisibleItem = linearLayoutManager.findFirstCompletelyVisibleItemPosition()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                app_bar.elevation = if (firstVisibleItem != 0) {
                    resources.getDimension(R.dimen.app_bar_elevation)
                } else {
                    0f
                }
            }
        }

        btn_verify.setOnClickListener {
            val seedMnemonic = verifySeedAdapter.enteredSeeds.joinToString(" ")
            try{
                multiWallet.verifySeed(wallet!!.walletID, seedMnemonic)
                val data = Intent(this, SeedBackupSuccess::class.java)
                data.putExtra(Constants.WALLET_ID, wallet!!.walletID)
                data.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                startActivity(data)
                finish()
            }catch (e: Exception){
                e.printStackTrace()
                SnackBar.showError(this, R.string.seed_verification_failed)
            }
        }

        go_back.setOnClickListener {
            finish()
        }
    }

    private fun prepareData() {

        val walletId = intent.getLongExtra(Constants.WALLET_ID, -1)
        wallet = multiWallet.getWallet(walletId)

        if (wallet!!.walletSeed.isNotBlank()){
            val seed = wallet!!.walletSeed
            seeds = seed!!.split(Constants.NBSP.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            initSeedAdapter()
        }
    }

    private fun getMultiSeedList(): ArrayList<MultiSeed> {
        val multiSeedList = ArrayList<MultiSeed>()
        for (seed in seeds) {
            multiSeedList.add(getMultiSeed(allSeeds.indexOf(seed)))
        }

        return multiSeedList
    }

    private fun getMultiSeed(realSeedIndex: Int): MultiSeed {

        val list = (0 until 33).toMutableList()
        list.remove(realSeedIndex)

        val firstRandom = list.random()
        val firstInputSeed = InputSeed(firstRandom, allSeeds[firstRandom])
        list.remove(firstRandom)

        val secondRandom = list.random()
        val secondInputSeed = InputSeed(secondRandom, allSeeds[secondRandom])

        val realInputSeed = InputSeed(realSeedIndex, allSeeds[realSeedIndex])

        val arr = arrayListOf(firstInputSeed, secondInputSeed, realInputSeed).apply { shuffle() }.toTypedArray()
        return MultiSeed(arr)
    }

    private fun initSeedAdapter() {
        val allSeedWords = getMultiSeedList()
        verifySeedAdapter = VerifySeedAdapter(this, allSeedWords){seedIndex ->
            linearLayoutManager.scrollToPosition(seedIndex + 2)
            btn_verify.isEnabled = verifySeedAdapter.allSeedsSelected
        }
        recycler_view_seeds.adapter = verifySeedAdapter
    }

}