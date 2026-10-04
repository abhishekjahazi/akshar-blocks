package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/**
 * The reward shop: the child's animal, big, and things to dress it in, bought with stars.
 * Tapping something owned puts it on or takes it off. The last tile opens the sticker album.
 */
class ShopView(context: Context, speaker: Speaker, player: Player) : GameView(context, speaker, player) {

    /** The sticker album tile was tapped. */
    var onAlbum: (() -> Unit)? = null

    override val title = "My shop"
    override val skyColor = Palette.LILAC

    private val items = Outfits.all
    private val tiles = Array(items.size + 1) { RectF() }
    private val stage = RectF()
    private val drawRect = RectF()
    private val shakeTime = FloatArray(items.size) { 1f }

    private var owned = player.ownedOutfits
    private var wearing = player.wearing

    /** When the animal last changed clothes, for a little bounce. */
    private var changedAt = -10f

    init {
        speaker.say(Outfits.ASK)
    }

    override fun update(dt: Float) {
        for (i in shakeTime.indices) shakeTime[i] += dt
    }

    override fun drawGame(canvas: Canvas) {
        val landscape = width > height
        val gap = dp(12f)
        val cols = 3
        val gridLeft: Float
        val gridTop: Float
        if (landscape) {
            stage.set(contentLeft, contentTop, contentLeft + (contentRight - contentLeft) * 0.36f, contentBottom - dp(10f))
            gridLeft = stage.right + dp(18f)
            gridTop = contentTop
        } else {
            stage.set(contentLeft, contentTop, contentRight, contentTop + (contentBottom - contentTop) * 0.36f)
            gridLeft = contentLeft
            gridTop = stage.bottom + dp(22f)
        }

        // The animal on its stage, bouncing when it gets something new.
        drawBlock(canvas, stage, Palette.WHITE, radius = dp(36f), depth = dp(10f), pressable = false)
        val bounce = popIn((time - changedAt) / 0.45f).let { if (time - changedAt < 0.45f) 0.85f + 0.15f * it else 1f }
        val face = min(stage.height() * 0.5f, stage.width() * 0.5f) * bounce
        drawAvatar(canvas, player.profile.avatar, wearing, stage.centerX(), stage.centerY() + face * 0.12f, face)

        val rows = (tiles.size + cols - 1) / cols
        val cellW = (contentRight - gridLeft) / cols
        val cellH = min((contentBottom - gridTop) / rows, cellW)
        for (i in tiles.indices) {
            val left = gridLeft + cellW * (i % cols)
            val top = gridTop + cellH * (i / cols)
            val tile = tiles[i]
            tile.set(left + gap / 2f, top + gap / 2f, left + cellW - gap / 2f, top + cellH - gap / 2f - dp(6f))
            val appear = popIn((time - 0.2f - i * 0.04f) / 0.4f)
            if (appear <= 0.01f) continue
            canvas.save()
            canvas.scale(appear, appear, tile.centerX(), tile.centerY())
            if (i == items.size) drawAlbumTile(canvas, tile) else drawItem(canvas, i, tile)
            canvas.restore()
        }
    }

    private fun drawItem(canvas: Canvas, i: Int, tile: RectF) {
        val item = items[i]
        val isOwned = item in owned
        val isWorn = item in wearing
        drawRect.set(tile)
        drawRect.offset(shakeOffset(shakeTime[i]), 0f)
        val face = when {
            isWorn -> Palette.SUN
            isOwned -> Palette.WHITE
            else -> 0xFFF1F3F9.toInt()
        }
        val sink = drawBlock(canvas, drawRect, face, radius = dp(20f), depth = dp(6f))
        val h = drawRect.height()
        drawEmoji(canvas, item.emoji, drawRect.centerX(), drawRect.top + h * 0.4f + sink, h * 0.42f)
        // Under it: the price, or a tick once it's the child's.
        val labelY = drawRect.top + h * 0.82f + sink
        if (isOwned) {
            drawEmoji(canvas, "✅", drawRect.centerX(), labelY, h * 0.16f)
        } else {
            val canBuy = player.spendableStars >= item.price
            // "⭐ 10", centered as a whole.
            val starSize = h * 0.15f
            val price = "${item.price}"
            textPaint.textSize = h * 0.17f
            val priceWidth = textPaint.measureText(price)
            val gap = starSize * 0.35f
            val left = drawRect.centerX() - (starSize + gap + priceWidth) / 2f
            drawEmoji(canvas, "⭐", left + starSize / 2f, labelY, starSize)
            drawText(canvas, price, left + starSize + gap + priceWidth / 2f, labelY, h * 0.17f, if (canBuy) Palette.INK else 0xFF8A94B0.toInt())
        }
    }

    private fun drawAlbumTile(canvas: Canvas, tile: RectF) {
        val sink = drawBlock(canvas, tile, Palette.GRAPE, radius = dp(20f), depth = dp(6f))
        val h = tile.height()
        drawEmoji(canvas, "📒", tile.centerX(), tile.top + h * 0.4f + sink, h * 0.42f)
        drawText(canvas, "Stickers", tile.centerX(), tile.top + h * 0.82f + sink, h * 0.15f, Palette.WHITE, tile.width() * 0.88f)
    }

    override fun onTap(x: Float, y: Float) {
        val i = tiles.indexOfFirst { it.contains(x, y) }
        if (i < 0) {
            if (stage.contains(x, y)) speaker.say(Outfits.ASK)
            return
        }
        if (i == items.size) {
            Sounds.play(Sound.TAP)
            onAlbum?.invoke()
            return
        }
        val item = items[i]
        when {
            item in owned -> {
                Sounds.play(Sound.TAP)
                wearing = Outfits.toggle(wearing, item)
                player.wearing = wearing
                changedAt = time
            }
            player.buy(item) -> {
                owned = player.ownedOutfits
                wearing = player.wearing
                changedAt = time
                Sounds.play(Sound.FANFARE)
                celebrate(stage.centerX(), stage.centerY())
                speaker.say(Outfits.bought(item))
            }
            else -> {
                Sounds.play(Sound.WRONG)
                shakeTime[i] = 0f
                speaker.say(Outfits.NEED_MORE)
            }
        }
    }
}
