package com.udacity

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var widthSize = 0
    private var heightSize = 0

    private val valueAnimator = ValueAnimator()
    private var loadingDefaultBackgroundColor = 0
    private var loadingBackgroundColor = 0
    private var loadingDefaultText: CharSequence = ""
    private var loadingText: CharSequence = ""
    private var loadingTextColor = 0
    private var progressCircleBackgroundColor = 0
    private var buttonText = ""
    private lateinit var buttonBackgroundAnimator: ValueAnimator
    private val buttonTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55f
        typeface = Typeface.DEFAULT
    }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private lateinit var buttonTextBounds: Rect
    // endregion
    private var currentButtonBackgroundAnimationValue = 0f
    // region Progress Circle/Arc variables
    private val progressCircleRect = RectF()
    private var progressCircleSize = 0f
    private val animatorSet: AnimatorSet = AnimatorSet().apply {
        duration = THREE_SECONDS
        disableViewDuringAnimation(this@LoadingButton)
    }


    private var buttonState: ButtonState by Delegates.observable<ButtonState>(ButtonState.Completed) { p, oldButtonState, newButtonState ->
        when (newButtonState) {
            ButtonState.Loading -> {
                // LoadingButton is now Loading and we need to set the correct text
                buttonText = loadingText.toString()

                // We only calculate ButtonText bounds and ProgressCircle rect once,
                // Only when buttonText is first initialized with loadingText
                if (!::buttonTextBounds.isInitialized) {
                    retrieveButtonTextBounds()
                    computeProgressCircleRect()
                }

    //             ProgressCircle and Button background animations must start now
                animatorSet.start()
            }
           else -> {
                // LoadingButton is not doing any Loading so we need to reset to default text
                buttonText = loadingDefaultText.toString()

                // ProgressCircle animation must stop now
                newButtonState.takeIf { it == ButtonState.Completed }?.run { animatorSet.cancel() }
            }
        }
    }
    private var currentProgressCircleAnimationValue = 0f
    private val progressCircleAnimator = ValueAnimator.ofFloat(0f, FULL_ANGLE).apply {
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            currentProgressCircleAnimationValue = it.animatedValue as Float
            invalidate()
        }
    }
    override fun performClick(): Boolean {
        super.performClick()
        // We only change button state to Clicked if the current state is Completed
        if (buttonState == ButtonState.Completed) {
            buttonState = ButtonState.Clicked
            invalidate()
        }
        return true
    }
    private fun retrieveButtonTextBounds() {
        buttonTextBounds = Rect()
        buttonTextPaint.getTextBounds(buttonText, 0, buttonText.length, buttonTextBounds)
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        progressCircleSize = (min(w, h) / BY_HALF) * PROGRESS_CIRCLE_SIZE_MULTIPLIER
        createButtonBackgroundAnimator()
    }
    /**
     * Calculate left, top, right and bottom for [progressCircleRect] based on [buttonTextBounds]
     * and [heightSize].
     *
     * **This needs to be called after [retrieveButtonTextBounds()] call**
     */
    private fun computeProgressCircleRect() {
        val horizontalCenter =
            (buttonTextBounds.right + buttonTextBounds.width() + PROGRESS_CIRCLE_LEFT_MARGIN_OFFSET)
        val verticalCenter = (heightSize / BY_HALF)

        progressCircleRect.set(
            horizontalCenter - progressCircleSize,
            verticalCenter - progressCircleSize,
            horizontalCenter + progressCircleSize,
            verticalCenter + progressCircleSize
        )
    }
    private fun createButtonBackgroundAnimator() {
        ValueAnimator.ofFloat(0f, widthSize.toFloat()).apply {
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                currentButtonBackgroundAnimationValue = it.animatedValue as Float
                invalidate()
            }
        }.also {
            buttonBackgroundAnimator = it
            animatorSet.playProgressCircleAndButtonBackgroundTogether()
        }
    }
    private fun AnimatorSet.playProgressCircleAndButtonBackgroundTogether() =
        apply { playTogether(progressCircleAnimator, buttonBackgroundAnimator) }

    init {
        isClickable = true
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            loadingDefaultBackgroundColor =
                getColor(R.styleable.LoadingButton_loadingDefaultBackgroundColor, 0)
            loadingBackgroundColor =
                getColor(R.styleable.LoadingButton_loadingBackgroundColor, 0)
            loadingDefaultText =
                getText(R.styleable.LoadingButton_loadingDefaultText)
            loadingTextColor =
                getColor(R.styleable.LoadingButton_loadingTextColor, 0)
            loadingText =
                getText(R.styleable.LoadingButton_loadingText)
        }.also {
            buttonText = loadingDefaultText.toString()
            progressCircleBackgroundColor = ContextCompat.getColor(context, R.color.colorAccent)
        }
    }

    fun changeButtonState(state: ButtonState) {
        if (state != buttonState) {
            buttonState = state
            invalidate()
        }
    }
   override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
       canvas?.let { buttonCanvas ->
           Timber.d("LoadingButton onDraw()")
           buttonCanvas.apply {
               drawBackgroundColor()
               drawButtonText()
               drawProgressCircleIfLoading()
           }
       }
   }
    private fun Canvas.drawButtonText() {
        // Draw the Loading Text at the Center of the Canvas
        // ref.: https://blog.danlew.net/2013/10/03/centering_single_line_text_in_a_canvas/
        buttonTextPaint.color = loadingTextColor
        drawText(
            buttonText,
            (widthSize / BY_HALF),
            (heightSize / BY_HALF) + buttonTextPaint.computeTextOffset(),
            buttonTextPaint
        )
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = resolveSizeAndState(minw, widthMeasureSpec, 1)
        val h: Int = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
       widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }

    /**
     * Draws the progress circle using an arc only when [buttonState] changes to [ButtonState.Loading].
     * The sweep angle uses [currentProgressCircleAnimationValue] which is changed according to when
     * [progressCircleAnimator] send updates after the values for the animation have been calculated.
     */
    private fun drawProgressCircle(buttonCanvas: Canvas) {
        buttonPaint.color = progressCircleBackgroundColor
        buttonCanvas.drawArc(
            progressCircleRect,
            0f,
            currentProgressCircleAnimationValue,
            true,
            buttonPaint
        )
    }
    private fun TextPaint.computeTextOffset() = ((descent() - ascent()) / 2) - descent()

    /**
     * Draws the default button background color using [loadingBackgroundColor].
     */
    private fun Canvas.drawBackgroundColor() {
        when (buttonState) {
            ButtonState.Loading -> {
                drawLoadingBackgroundColor()
                drawDefaultBackgroundColor()
            }
            else -> drawColor(loadingDefaultBackgroundColor)
        }
    }
    private fun Canvas.drawLoadingBackgroundColor() = buttonPaint.apply {
        color = loadingBackgroundColor
    }.run {
        drawRect(
            0f,
            0f,
            currentButtonBackgroundAnimationValue,
            heightSize.toFloat(),
            buttonPaint
        )
    }

    /**
     * Draws the [Rect] with [loadingDefaultBackgroundColor] representing the background of the button.
     */
    private fun Canvas.drawDefaultBackgroundColor() = buttonPaint.apply {
        color = loadingDefaultBackgroundColor
    }.run {
        drawRect(
            currentButtonBackgroundAnimationValue,
            0f,
            widthSize.toFloat(),
            heightSize.toFloat(),
            buttonPaint
        )
    }

    /**
     * Draws progress circle if [buttonState] is [ButtonState.Loading].
     */
    private fun Canvas.drawProgressCircleIfLoading() =
        buttonState.takeIf { it == ButtonState.Loading }?.let { drawProgressCircle(this) }

    /**
     * Draws the progress circle using an arc only when [buttonState] changes to [ButtonState.Loading].
     * The sweep angle uses [currentProgressCircleAnimationValue] which is changed according to when
     * [progressCircleAnimator] send updates after the values for the animation have been calculated.
     */

    companion object {
        private const val PROGRESS_CIRCLE_SIZE_MULTIPLIER = 0.4f
        private const val PROGRESS_CIRCLE_LEFT_MARGIN_OFFSET = 16f
        private const val BY_HALF = 2f
        private const val FULL_ANGLE = 360f
        private val THREE_SECONDS = TimeUnit.SECONDS.toMillis(3)
    }
}