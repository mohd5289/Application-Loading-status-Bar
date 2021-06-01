package com.udacity

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import com.udacity.databinding.ActivityDetailBinding
import com.udacity.databinding.ContentDetailBinding
import kotlinx.android.synthetic.main.activity_detail.*

class DetailActivity : AppCompatActivity() {
    private val fileName by lazy {
        intent?.extras?.getString(EXTRA_FILE_NAME, unknownText) ?: unknownText
    }
    private val downloadStatus by lazy {
        intent?.extras?.getString(EXTRA_DOWNLOAD_STATUS, unknownText) ?: unknownText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    DataBindingUtil.setContentView<ActivityDetailBinding>(this, R.layout.activity_detail).apply {
          setSupportActionBar(toolbar)
          contentDetail.initializeView()
      }





    }
    private val unknownText by lazy { getString(R.string.unknown) }

    private fun ContentDetailBinding.initializeView() {
        fileNameText.text = fileName
        downloadStatusText.text = downloadStatus
        okButton.setOnClickListener { finish() }
        changeViewForDownloadStatus()
    }
    private fun ContentDetailBinding.changeViewForDownloadStatus() {
        when (downloadStatusText.text) {
            DownloadStatus.SUCCESSFUL.statusText -> {
                changeDownloadStatusImageTo(R.drawable.ic_check_circle_outline_24)
                changeDownloadStatusColorTo(R.color.colorPrimaryDark)
            }
            DownloadStatus.FAILED.statusText -> {
                changeDownloadStatusImageTo(R.drawable.ic_error_24)
                changeDownloadStatusColorTo(R.color.design_default_color_error)
            }
        }
    }
    private fun ContentDetailBinding.changeDownloadStatusImageTo(@DrawableRes imageRes: Int) {
        downloadStatusImage.setImageResource(imageRes)
    }

    private fun ContentDetailBinding.changeDownloadStatusColorTo(@ColorRes colorRes: Int) {
        ContextCompat.getColor(this@DetailActivity, colorRes)
            .also { color ->
                downloadStatusImage.imageTintList = ColorStateList.valueOf(color)
                downloadStatusText.setTextColor(color)
            }
    }


    companion object {
        private const val EXTRA_FILE_NAME = "${BuildConfig.APPLICATION_ID}.FILE_NAME"
        private const val EXTRA_DOWNLOAD_STATUS = "${BuildConfig.APPLICATION_ID}.DOWNLOAD_STATUS"
        fun bundleExtrasOf(
            fileName: String,
            downloadStatus: DownloadStatus
        ) = bundleOf(
            EXTRA_FILE_NAME to fileName,
            EXTRA_DOWNLOAD_STATUS to downloadStatus.statusText
        )
    }
}
