package quanticheart.com.cameraview.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import quanticheart.com.cameraview.R

class FlashImageView2 @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var auto_on: AnimatedVectorDrawableCompat? = null
    private var on_off: AnimatedVectorDrawableCompat? = null
    private var off_auto: AnimatedVectorDrawableCompat? = null

    init {
        auto_on = AnimatedVectorDrawableCompat.create(context, R.drawable.ic_flash_auto_on)
        on_off = AnimatedVectorDrawableCompat.create(context, R.drawable.ic_flash_on_off)
        off_auto = AnimatedVectorDrawableCompat.create(context, R.drawable.ic_flash_off_auto)
        setImageDrawable(off_auto)
    }

    fun flashAutoToOn() {
        val drawable = auto_on
        setImageDrawable(drawable)
        drawable?.start()
    }

    fun flashOnToOff() {
        val drawable = on_off
        setImageDrawable(drawable)
        drawable?.start()
    }

    fun flashOffToAuto() {
        val drawable = off_auto
        setImageDrawable(drawable)
        drawable?.start()
    }

}