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
         * 默认文字大小，单位：sp。
         */
        private const val DEFAULT_TEXT_SIZE = 16f

        /**
         * 默认闪烁次数。
         */
        private const val DEFAULT_BLINK_COUNT = 3

        /**
         * 默认单次亮/暗时间，单位：ms。
         */
        private const val DEFAULT_BLINK_DURATION = 200L

        /**
         * 默认左右内边距，单位：dp。
         */
        private const val DEFAULT_HORIZONTAL_PADDING = 4f

        /**
         * 默认上下内边距，单位：dp。
         */
        private const val DEFAULT_VERTICAL_PADDING = 2f
    }

    /**
     * 显示文字。
     */
    private var text: String = ""

    /**
     * 文字颜色。
     */
    @ColorInt
    private var textColor: Int = 0xFFFFFFFF.toInt()

    /**
     * 文字大小。
     *
     * 保存的是 SP 数值，不是 PX。
     */
    private var textSizeSp: Float = DEFAULT_TEXT_SIZE

    /**
     * 默认闪烁次数。
     */
    private var blinkCount: Int = DEFAULT_BLINK_COUNT

    /**
     * 单次亮/暗的持续时间。
     */
    private var blinkDuration: Long = DEFAULT_BLINK_DURATION

    /**
     * 水平内边距。
     *
     * 单位：px。
     */
    var horizontalPadding: Float = dp(DEFAULT_HORIZONTAL_PADDING)
        set(value) {
            field = value.coerceAtLeast(0f)
            requestLayout()
            invalidate()
        }

    /**
     * 垂直内边距。
     *
     * 单位：px。
     */
    var verticalPadding: Float = dp(DEFAULT_VERTICAL_PADDING)
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

    /**
     * Paint。
     */
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    init {
        // 先设置默认 Paint 参数
        paint.color = textColor
        paint.textSize = sp(DEFAULT_TEXT_SIZE)

        context.obtainStyledAttributes(
            attrs,
            R.styleable.DoraBreathingView,
            defStyleAttr,
            0
        ).apply {

            /**
             * 文字。
             */
            text = getString(
                R.styleable.DoraBreathingView_dview_bv_text
            ).orEmpty()

            /**
             * 文字颜色。
             */
            textColor = getColor(
                R.styleable.DoraBreathingView_dview_bv_textColor,
                textColor
            )

            /**
             * 文字大小。
             *
             * getDimension() 返回 px，
             * 所以转换回 SP 保存。
             */
            textSizeSp = getDimension(
                R.styleable.DoraBreathingView_dview_bv_textSize,
                sp(DEFAULT_TEXT_SIZE)
            ) / resources.displayMetrics.scaledDensity

            /**
             * 关键：
             * XML 属性解析完成后，
             * 必须同步更新 Paint。
             */
            paint.color = textColor
            paint.textSize = sp(textSizeSp)

            /**
             * 闪烁次数。
             */
            blinkCount = getInt(
                R.styleable.DoraBreathingView_dview_bv_blinkCount,
                DEFAULT_BLINK_COUNT
            ).coerceAtLeast(1)

            /**
             * 闪烁时间。
             */
            blinkDuration = getInt(
                R.styleable.DoraBreathingView_dview_bv_blinkDuration,
                DEFAULT_BLINK_DURATION.toInt()
            )
                .toLong()
                .coerceAtLeast(50L)

            /**
             * 水平内边距。
             */
            horizontalPadding = getDimension(
                R.styleable.DoraBreathingView_dview_bv_horizontalPadding,
                dp(DEFAULT_HORIZONTAL_PADDING)
            )

            /**
             * 垂直内边距。
             */
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

        val textWidth = if (text.isEmpty()) {
            0f
        } else {
            paint.measureText(text)
        }

        val textHeight = fontMetrics.bottom - fontMetrics.top

        val desiredWidth = (
            textWidth + horizontalPadding * 2
        ).toInt()

        val desiredHeight = (
            textHeight + verticalPadding * 2
        ).toInt()

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
        val y = height / 2f -
                (fontMetrics.ascent + fontMetrics.descent) / 2f

        /**
         * 根据当前动画透明度绘制。
         */
        paint.alpha = (
            currentAlpha * 255f
        )
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
        requestLayout()
        invalidate()
    }

    /**
     * 获取当前文字。
     */
    fun getText(): String {
        return text
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
        setText(value.toString())
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
        setText("$prefix$value")
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
        setText("$prefix$value$suffix")
    }

    /**
     * 设置文字颜色。
     */
    fun setTextColor(
        @ColorInt color: Int
    ) {
        textColor = color
        paint.color = color
        invalidate()
    }

    /**
     * 获取文字颜色。
     */
    @ColorInt
    fun getTextColor(): Int {
        return textColor
    }

    /**
     * 设置文字大小。
     *
     * 单位：sp。
     */
    fun setTextSizeSp(size: Float) {
        textSizeSp = size.coerceAtLeast(0f)

        /**
         * Paint 使用 px，
         * 所以这里需要 SP -> PX。
         */
        paint.textSize = sp(textSizeSp)

        requestLayout()
        invalidate()
    }

    /**
     * 获取文字大小。
     *
     * 单位：sp。
     */
    fun getTextSizeSp(): Float {
        return textSizeSp
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
        blinkCount = count.coerceAtLeast(1)
    }

    /**
     * 获取闪烁次数。
     */
    fun getBlinkCount(): Int {
        return blinkCount
    }

    /**
     * 设置闪烁速度。
     *
     * 单位：ms。
     */
    fun setBlinkDuration(duration: Long) {
        blinkDuration = duration.coerceAtLeast(50L)
    }

    /**
     * 获取闪烁速度。
     */
    fun getBlinkDuration(): Long {
        return blinkDuration
    }

    /**
     * 开始默认次数的闪烁。
     *
     * 默认：
     *
     * 亮 -> 暗 -> 亮
     *
     * 共 3 次。
     *
     * 完成后自动隐藏。
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
     *
     * 完成后自动隐藏。
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

        /**
         * 开始时重新显示。
         */
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

                    /**
                     * 动画正常结束。
                     */
                    override fun onAnimationEnd(
                        animation: Animator
                    ) {
                        if (animation != animator) {
                            return
                        }

                        animator = null
                        blinking = false

                        /**
                         * 最终隐藏。
                         */
                        currentAlpha = 0f

                        invalidate()

                        onBlinkEnd?.invoke()
                    }

                    /**
                     * 动画被取消。
                     *
                     * 取消不是正常完成，
                     * 所以恢复显示。
                     */
                    override fun onAnimationCancel(
                        animation: Animator
                    ) {
                        if (animation != animator) {
                            return
                        }

                        animator = null
                        blinking = false

                        /**
                         * 取消动画时恢复显示。
                         */
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
     *
     * 停止后恢复显示。
     */
    fun stopBlink() {
        animator?.cancel()

        animator = null
        blinking = false

        /**
         * 手动停止后保持显示。
         */
        currentAlpha = 1f

        invalidate()
    }

    /**
     * 立即隐藏。
     */
    fun hide() {
        animator?.cancel()

        animator = null
        blinking = false

        currentAlpha = 0f

        invalidate()
    }

    /**
     * 立即显示。
     */
    fun show() {
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

    /**
     * 当前是否可见。
     *
     * 注意：
     * 这里判断的是控件内部文字透明度，
     * 不是 View.visibility。
     */
    fun isShownByAlpha(): Boolean {
        return currentAlpha > 0f
    }

    override fun onDetachedFromWindow() {
        stopBlink()
        super.onDetachedFromWindow()
    }

    /**
     * dp -> px。
     */
    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        )
    }

    /**
     * sp -> px。
     */
    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            value,
            resources.displayMetrics
        )
    }
}
