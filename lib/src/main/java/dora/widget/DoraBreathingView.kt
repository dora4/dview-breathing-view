package dora.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.ColorInt
import dora.widget.breathingview.R

/**
 * 呼吸灯控件。
 *
 * 通过透明度变化实现：
 *
 * 亮 -> 暗 -> 亮
 *
 * 默认闪烁 3 次。
 *
 * 适用于：
 *
 * - 数值变化提示
 * - 状态提示
 * - 通知提示
 * - 游戏数值
 * - 经验、金币、积分
 * - 宠物属性变化
 * - 任意需要强调显示的文字
 *
 * 例如：
 *
 * +10
 * +100
 * -5
 * 100%
 * NEW
 * 3
 */
class DoraBreathingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {

        /**
         * 默认文字大小。
         */
        private const val DEFAULT_TEXT_SIZE = 16f

        /**
         * 默认闪烁次数。
         */
        private const val DEFAULT_BLINK_COUNT = 3

        /**
         * 默认单次亮/暗时间。
         */
        private const val DEFAULT_BLINK_DURATION = 200L

        /**
         * 默认左右内边距。
         */
        private const val DEFAULT_HORIZONTAL_PADDING = 4f

        /**
         * 默认上下内边距。
         */
        private const val DEFAULT_VERTICAL_PADDING = 2f
    }

    /**
     * 显示文字。
     */
    var text: String = ""
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    /**
     * 文字颜色。
     */
    @ColorInt
    var textColor: Int = 0xFFFFFFFF.toInt()
        set(value) {
            field = value
            paint.color = value
            invalidate()
        }

    /**
     * 文字大小。
     *
     * 单位：sp。
     */
    var textSizeSp: Float = DEFAULT_TEXT_SIZE
        set(value) {
            field = value.coerceAtLeast(0f)
            paint.textSize = sp(field)
            requestLayout()
            invalidate()
        }

    /**
     * 默认闪烁次数。
     *
     * blink() 会使用此值。
     */
    var blinkCount: Int = DEFAULT_BLINK_COUNT
        set(value) {
            field = value.coerceAtLeast(1)
        }

    /**
     * 单次亮/暗的持续时间。
     *
     * 例如 200ms：
     *
     * 亮 -> 200ms -> 暗 -> 200ms -> 亮
     */
    var blinkDuration: Long = DEFAULT_BLINK_DURATION
        set(value) {
            field = value.coerceAtLeast(50L)
        }

    /**
     * 水平内边距。
     */
    var horizontalPadding: Float = DEFAULT_HORIZONTAL_PADDING
        set(value) {
            field = value.coerceAtLeast(0f)
            requestLayout()
            invalidate()
        }

    /**
     * 垂直内边距。
     */
    var verticalPadding: Float = DEFAULT_VERTICAL_PADDING
        set(value) {
            field = value.coerceAtLeast(0f)
            requestLayout()
            invalidate()
        }

    /**
     * 当前透明度。
     */
    private var currentAlpha = 1f

    /**
     * 当前是否正在执行闪烁。
     */
    private var blinking = false

    /**
     * 当前动画。
     */
    private var animator: ValueAnimator? = null

    /**
     * 闪烁开始回调。
     */
    var onBlinkStart: (() -> Unit)? = null

    /**
     * 闪烁结束回调。
     */
    var onBlinkEnd: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        color = textColor
        textSize = sp(DEFAULT_TEXT_SIZE)
    }

    init {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.DoraBreathingView,
            defStyleAttr,
            0
        ).apply {
            text = getString(
                R.styleable.DoraBreathingView_dview_bv_text
            ).orEmpty()
            textColor = getColor(
                R.styleable.DoraBreathingView_dview_bv_textColor,
                textColor
            )
            textSizeSp = getDimension(
                R.styleable.DoraBreathingView_dview_bv_textSize,
                sp(DEFAULT_TEXT_SIZE)
            ) / resources.displayMetrics.scaledDensity
            blinkCount = getInt(
                R.styleable.DoraBreathingView_dview_bv_blinkCount,
                DEFAULT_BLINK_COUNT
            )
            blinkDuration = getInt(
                R.styleable.DoraBreathingView_dview_bv_blinkDuration,
                DEFAULT_BLINK_DURATION.toInt()
            ).toLong()
            horizontalPadding = getDimension(
                R.styleable.DoraBreathingView_dview_bv_horizontalPadding,
                dp(DEFAULT_HORIZONTAL_PADDING)
            )
            verticalPadding = getDimension(
                R.styleable.DoraBreathingView_dview_bv_verticalPadding,
                dp(DEFAULT_VERTICAL_PADDING)
            )
            recycle()
        }
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val fontMetrics = paint.fontMetrics
        val textWidth = paint.measureText(text)
        val textHeight = fontMetrics.bottom - fontMetrics.top
        val desiredWidth = (textWidth + horizontalPadding * 2).toInt()
        val desiredHeight = (textHeight + verticalPadding * 2).toInt()
        val width = resolveSize(
            desiredWidth,
            widthMeasureSpec
        )
        val height = resolveSize(
            desiredHeight,
            heightMeasureSpec
        )
        setMeasuredDimension(
            width,
            height
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (text.isEmpty()) {
            return
        }
        val fontMetrics = paint.fontMetrics
        val x = width / 2f
        /**
         * 让文字在 View 中垂直居中。
         */
        val y = height / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
        paint.alpha = (currentAlpha * 255f)
                .toInt()
                .coerceIn(0, 255)
        canvas.drawText(
            text,
            x,
            y,
            paint
        )
        /**
         * 避免 alpha 状态影响下一次绘制。
         */
        paint.alpha = 255
    }

    /**
     * 设置显示文字。
     *
     * 例如：
     *
     * setText("+10")
     */
    fun setText(text: String) {
        this.text = text
    }

    /**
     * 设置数值。
     *
     * 例如：
     *
     * setValue(10)
     *
     * 显示：
     *
     * 10
     */
    fun setValue(value: Int) {
        text = value.toString()
    }

    /**
     * 设置数值并添加前缀。
     *
     * 例如：
     *
     * setValue(10, "+")
     *
     * 显示：
     *
     * +10
     */
    fun setValue(
        value: Int,
        prefix: String
    ) {
        text = "$prefix$value"
    }

    /**
     * 设置数值，同时支持前缀和后缀。
     *
     * 例如：
     *
     * setValue(
     *     value = 10,
     *     prefix = "+",
     *     suffix = " HP"
     * )
     *
     * 显示：
     *
     * +10 HP
     */
    fun setValue(
        value: Int,
        prefix: String = "",
        suffix: String = ""
    ) {
        text = "$prefix$value$suffix"
    }

    /**
     * 设置文字颜色。
     */
    fun setTextColorInt(
        @ColorInt color: Int
    ) {
        textColor = color
    }

    /**
     * 设置文字大小。
     *
     * 单位：sp。
     */
    fun setTextSizeSp(size: Float) {
        textSizeSp = size
    }

    /**
     * 设置文字样式。
     */
    fun setTypeface(typeface: Typeface?) {
        paint.typeface = typeface ?: Typeface.DEFAULT
        requestLayout()
        invalidate()
    }

    /**
     * 设置闪烁次数。
     */
    fun setBlinkCount(count: Int) {
        blinkCount = count
    }

    /**
     * 设置闪烁速度。
     */
    fun setBlinkDuration(duration: Long) {
        blinkDuration = duration
    }

    /**
     * 开始默认次数的闪烁。
     *
     * 默认：
     *
     * 亮 -> 暗 -> 亮
     *
     * 共 3 次。
     */
    fun blink() {
        blink(blinkCount)
    }

    /**
     * 指定闪烁次数。
     *
     * 例如：
     *
     * blink(5)
     */
    fun blink(count: Int) {
        if (count <= 0) {
            return
        }
        /**
         * 如果当前正在执行动画，
         * 先取消当前动画。
         */
        animator?.cancel()
        blinking = true
        currentAlpha = 1f
        invalidate()
        onBlinkStart?.invoke()
        animator = ValueAnimator.ofFloat(
            1f,
            0f,
            1f
        ).apply {
            /**
             * 一次完整循环：
             *
             * 亮 -> 暗 -> 亮
             */
            duration = blinkDuration * 2
            /**
             * count = 3：
             *
             * 第 1 次：亮 -> 暗 -> 亮
             * 第 2 次：亮 -> 暗 -> 亮
             * 第 3 次：亮 -> 暗 -> 亮
             */
            repeatCount = count - 1
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                currentAlpha = it.animatedValue as Float
                invalidate()
            }
            addListener(
                object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(
                        animation: Animator
                    ) {
                        if (animation != animator) {
                            return
                        }
                        animator = null
                        blinking = false
                        currentAlpha = 1f
                        invalidate()
                        onBlinkEnd?.invoke()
                    }

                    override fun onAnimationCancel(
                        animation: Animator
                    ) {
                        if (animation != animator) {
                            return
                        }
                        animator = null
                        blinking = false
                        currentAlpha = 1f
                        invalidate()
                    }
                }
            )
            start()
        }
    }

    /**
     * 停止当前闪烁。
     */
    fun stopBlink() {
        animator?.cancel()
        animator = null
        blinking = false
        currentAlpha = 1f
        invalidate()
    }

    /**
     * 当前是否正在闪烁。
     */
    fun isBlinking(): Boolean {
        return blinking
    }

    override fun onDetachedFromWindow() {
        stopBlink()
        super.onDetachedFromWindow()
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            value,
            resources.displayMetrics
        )
    }
}
