package com.mxchip.mxapp.pair.weidget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import com.mxchip.mxapp.base.utils.dp2px

/**
 * 扫描水波纹效果
 */
class WaterWaveView : View {

    //初始波纹半径,波纹生成时的半径
    private val waveRadiusMin = 30.dp2px().toFloat()

    //波纹消失前的半径
    private var mWaveRadiusMax = 0f

    //每条波纹持续时间
    private var mWaveDuration: Long = 4500

    //波纹生成速度
    private var mWaveCreatedSpeed = 1000L

    private val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.strokeWidth = 3f
        this.isDither = true
        this.style = Paint.Style.FILL
    }

    /**
     * 设置水波纹效果
     *
     * 0 PathInterpolator 可以定义路径坐标，然后可以按照路径坐标来
     * 1 AccelerateDecelerateInterpolator 在动画开始与结束的地方速率改变比较慢，在中间的时候加速
     * 2 AccelerateInterpolator  在动画开始的地方速率改变比较慢，然后开始加速
     * 3 AnticipateInterpolator 开始的时候向后然后向前甩
     * 4 AnticipateOvershootInterpolator 开始的时候向后然后向前甩一定值后返回最后的值
     * 5 BounceInterpolator   动画结束的时候弹起
     * 6 CycleInterpolator 动画循环播放特定的次数，速率改变沿着正弦曲线
     * 7 DecelerateInterpolator 在动画开始的地方快然后慢
     * 8 LinearInterpolator   以常量速率改变
     * 9 OvershootInterpolator    向前甩一定值后再回到原来位置
     */
    private var mInterpolator = AccelerateInterpolator()

    //所有的水波纹
    private val mAnimatorList = ArrayList<ValueAnimator>()

    //是否开启水波纹
    private var isStart = false

    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null

    private val mWaveRunable: Runnable by lazy {
        object : Runnable {
            override fun run() {
                if (isStart) {
                    newWaveAnimator()
                    invalidate()
                    postDelayed(this, mWaveCreatedSpeed)
                }
            }
        }
    }

    constructor(context: Context) : super(context)

    constructor(
        context: Context,
        attrs: AttributeSet? = null
    ) : super(context, attrs)

    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : super(context, attrs, defStyleAttr)

    //设置水波纹颜色
    fun setColor(color: Int) {
        mPaint.color = color
    }

    //开启动画
    fun start() {
        if (!isStart) {
            isStart = true
            mWaveRunable.run()
        }
    }

    //关闭动画
    fun stop() {
        isStart = false
        clearAnimation()
        bitmap?.apply {
            if (this.isRecycled)
                this.recycle()
        }
    }

    /**
     * 暂停
     */
    fun pause() {
        isStart = false
        for (i in mAnimatorList.indices) {
            val mWaveAnimator: ValueAnimator = mAnimatorList[i]
            mWaveAnimator.pause()
        }
    }

    /**
     * 继续
     */
    fun resume() {
        isStart = true
        for (i in mAnimatorList.indices) {
            val mWaveAnimator: ValueAnimator = mAnimatorList[i]
            mWaveAnimator.resume()
        }
        mWaveRunable.run()
    }

    private fun newWaveAnimator(): ValueAnimator {
        val mWaveAnimator = ValueAnimator()
        mWaveAnimator.setFloatValues(waveRadiusMin, mWaveRadiusMax)
        mWaveAnimator.duration = mWaveDuration
        mWaveAnimator.repeatCount = 0
        mWaveAnimator.interpolator = mInterpolator
        mAnimatorList.add(mWaveAnimator)
        mWaveAnimator.start()
        return mWaveAnimator
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        mWaveRadiusMax = (w.coerceAtLeast(h) * 0.8).toFloat()
        bitmap = Bitmap.createBitmap(
            this.measuredWidth,
            this.measuredHeight,
            Bitmap.Config.ARGB_4444
        )
        bitmap?.apply {
            bitmapCanvas = Canvas(this)
        }
    }

    override fun onDraw(canvas: Canvas) {
        bitmapCanvas?.let { btCanvas ->
            btCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            val valueAnimators = mutableListOf<ValueAnimator>()
            for (i in mAnimatorList.indices) {
                val valueAnimator = mAnimatorList[i]
                if (valueAnimator.isRunning && valueAnimator.animatedValue != mWaveRadiusMax) {
                    val current = if (i == mAnimatorList.size - 1) {
                        waveRadiusMin
                    } else {
                        mAnimatorList[i + 1].animatedValue as Float
                    }
                    //设置透明度
                    //画水波纹
                    if (valueAnimator.animatedValue as Float > waveRadiusMin) {
                        mPaint.alpha = getAlpha(valueAnimator.animatedValue as Float)
                        mPaint.style = Paint.Style.STROKE
                        mPaint.strokeWidth = valueAnimator.animatedValue as Float - current
                        bitmapCanvas!!.drawCircle(
                            (measuredWidth / 2).toFloat(),
                            measuredHeight - waveRadiusMin - 20,
                            valueAnimator.animatedValue as Float / 2 + current / 2,
                            mPaint
                        )
                    }
                } else {
                    valueAnimator.cancel()
                    valueAnimators.add(valueAnimator)
                }
            }
            mAnimatorList.removeAll(valueAnimators)
            mPaint.alpha = 35
            mPaint.style = Paint.Style.FILL
            mPaint.strokeWidth = 3f
            bitmapCanvas!!.drawCircle(
                (measuredWidth / 2).toFloat(),
                measuredHeight - waveRadiusMin - 20,
                waveRadiusMin,
                mPaint
            )
            //将bitmap绘制到画板上
            canvas.drawBitmap(bitmap!!, 0f, 0f, Paint())
            if (mAnimatorList.size > 0) {
                postInvalidateDelayed(10)
            }
        }
    }

    //获取水波纹透明度
    private fun getAlpha(mRadius: Float): Int {
        var alpha = 1
        if (mWaveRadiusMax > 0) {
            alpha =
                ((1 - (mRadius - waveRadiusMin) / (mWaveRadiusMax - waveRadiusMin)) * 0.1 * 255).toInt()
        }
        return alpha
    }
}