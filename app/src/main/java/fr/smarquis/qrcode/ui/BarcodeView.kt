package fr.smarquis.qrcode.ui

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import fr.smarquis.qrcode.R
import fr.smarquis.qrcode.databinding.ViewBarcodeBinding
import fr.smarquis.qrcode.model.Barcode

class BarcodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewBarcodeBinding.inflate(LayoutInflater.from(context), this)

    private val bottomSheetBehavior by lazy { BottomSheetBehavior.from(binding.root) }

    private lateinit var onCollapsed: () -> Unit
    private lateinit var open: (Barcode) -> Unit
    private lateinit var copy: (Barcode) -> Unit
    private var showMore: (() -> Unit)? = null

    var barcode: Barcode? = null
        set(value) {
            field = value.also { if (it == field) return }
            if (value != null) render(value) else clear()
        }

    private val callback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == STATE_COLLAPSED) {
                onCollapsed()
            }
        }
    }

    init {
        isClickable = true
        clipChildren = false
        orientation = VERTICAL
        with(binding) {
            more.setOnClickListener { showMore?.invoke() }
            open.setOnClickListener { barcode?.let { open(it) } }
            copy.setOnClickListener { barcode?.let { copy(it) } }
            header.setOnClickListener {
                barcode ?: return@setOnClickListener
                bottomSheetBehavior.state = when (bottomSheetBehavior.state) {
                    STATE_COLLAPSED -> STATE_EXPANDED
                    STATE_EXPANDED -> STATE_COLLAPSED
                    else -> return@setOnClickListener
                }
            }
            listOf(header, body).forEach {
                it.layoutTransition = LayoutTransition().apply {
                    setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
                    setAnimateParentHierarchy(false) // Required by BottomSheetBehavior
                }
            }
        }
    }

    fun anchor(): ImageView = binding.more

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bottomSheetBehavior.addBottomSheetCallback(callback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bottomSheetBehavior.removeBottomSheetCallback(callback)
    }

    fun configure(onCollapsed: () -> Unit, showMore: (() -> Unit)? = null, open: (Barcode) -> Unit, copy: (Barcode) -> Unit) {
        this.onCollapsed = onCollapsed
        this.showMore = showMore.also {
            binding.more.isVisible = it != null
        }
        this.open = open
        this.copy = copy
    }

    private fun clear() = with(binding) {
        bottomSheetBehavior.state = STATE_COLLAPSED
        progress.isVisible = true
        icon.isGone = true
        icon.setImageResource(0)
        title.setText(R.string.status_scanning)
        open.isGone = true
        copy.isGone = true
        details.isGone = true
        raw.isGone = true
    }

    private fun render(barcode: Barcode) = with(binding) {
        progress.isGone = true
        icon.setImageResource(barcode.icon)
        icon.isVisible = true
        title.text = barcode.title
        open.isVisible = barcode.intent != null
        copy.isVisible = true
        barcode.details.let {
            details.isGone = it.isNullOrBlank()
            details.text = it
        }
        barcode.value.let {
            raw.isGone = it.isBlank()
            raw.text = it
        }
        bottomSheetBehavior.state = STATE_EXPANDED
    }

}
