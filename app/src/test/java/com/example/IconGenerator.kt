package com.example

import org.junit.Test
import java.io.File
import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class IconGenerator {

    @Test
    fun generateAppIcons() {
        val size = 512
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        
        // Enable high-quality anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        // Clear background to transparent
        g.composite = AlphaComposite.Clear
        g.fillRect(0, 0, size, size)
        g.composite = AlphaComposite.SrcOver

        // Center and scale the entire content to prevent the "zoomed-in" look on devices (using safe-zone standard of ~72%)
        val scaleFactor = 0.72
        val offset = (size * (1.0 - scaleFactor)) / 2.0
        g.translate(offset, offset)
        g.scale(scaleFactor, scaleFactor)

        // Brand color: Sky Blue (#73A8D7 or #6BA4D9)
        val brandColor = Color(115, 168, 215)
        
        // 1. Draw P Stem (Rounded Rectangle)
        val stemWidth = size * 0.055f
        val stemX = size * 0.23f
        val stemTop = size * 0.15f
        val stemBottom = size * 0.95f
        
        g.color = brandColor
        g.fill(RoundRectangle2D.Float(
            stemX, 
            stemTop, 
            stemWidth, 
            stemBottom - stemTop, 
            stemWidth, 
            stemWidth
        ))
        
        // 2. Draw P Bowl (Circle ring)
        val bowlRadius = size * 0.28f
        val bowlCenterX = stemX + (stemWidth / 2f) + bowlRadius
        val bowlCenterY = stemTop + bowlRadius
        
        val bowlStrokeWidth = size * 0.055f
        val bowlDrawRadius = bowlRadius - (bowlStrokeWidth / 2f)
        
        g.stroke = BasicStroke(bowlStrokeWidth)
        g.draw(Ellipse2D.Float(
            bowlCenterX - bowlDrawRadius, 
            bowlCenterY - bowlDrawRadius, 
            bowlDrawRadius * 2f, 
            bowlDrawRadius * 2f
        ))
        
        // 3. Draw Checkmark Mask (Erase a beautiful gap over the circle)
        val innerRadius = bowlRadius - (size * 0.055f)
        
        val checkStartX = bowlCenterX - (bowlRadius * 0.60f)
        val checkStartY = bowlCenterY
        
        val checkCornerX = bowlCenterX - (bowlRadius * 0.15f)
        val checkCornerY = bowlCenterY + (bowlRadius * 0.45f)
        
        val checkEndX = bowlCenterX + (bowlRadius * 1.20f)
        val checkEndY = bowlCenterY - (bowlRadius * 0.90f)
        
        val checkStrokeWidth = size * 0.055f
        val maskStrokeWidth = checkStrokeWidth * 1.8f
        
        val checkPath = Path2D.Float()
        checkPath.moveTo(checkStartX, checkStartY)
        checkPath.lineTo(checkCornerX, checkCornerY)
        checkPath.lineTo(checkEndX, checkEndY)
        
        // Clear the mask area (draw transparent/white gap in the circle)
        g.stroke = BasicStroke(maskStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.composite = AlphaComposite.DstOut
        g.draw(checkPath)
        
        // Restore composite
        g.composite = AlphaComposite.SrcOver
        
        // 4. Draw Checkmark itself
        g.color = brandColor
        g.stroke = BasicStroke(checkStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(checkPath)
        
        g.dispose()
        
        // Save foreground transparent icon
        var rootDir = File(".").absoluteFile
        while (rootDir != null && !File(rootDir, "settings.gradle.kts").exists()) {
            rootDir = rootDir.parentFile
        }
        
        val resDir = File(rootDir, "app/src/main/res")
        resDir.mkdirs()
        
        ImageIO.write(image, "PNG", File(resDir, "drawable/ic_launcher_foreground.png"))
        println("GENERATED FOREGROUND ICON AT: " + File(resDir, "drawable/ic_launcher_foreground.png").absolutePath)

        // Now generate complete icons with solid white background
        val mipmapDirs = listOf(
            "mipmap-mdpi" to 48,
            "mipmap-hdpi" to 72,
            "mipmap-xhdpi" to 96,
            "mipmap-xxhdpi" to 144,
            "mipmap-xxxhdpi" to 192
        )
        
        mipmapDirs.forEach { (dirName, targetSize) ->
            val iconImage = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB)
            val ig = iconImage.createGraphics()
            ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            ig.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            ig.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            
            // Draw solid white background
            ig.color = Color.WHITE
            ig.fillRect(0, 0, targetSize, targetSize)
            
            // Draw the logo centered.
            val scale = targetSize.toDouble() / 512.0
            val transform = AffineTransform.getScaleInstance(scale, scale)
            ig.drawImage(image, transform, null)
            ig.dispose()
            
            val targetDir = File(resDir, dirName)
            targetDir.mkdirs()
            ImageIO.write(iconImage, "PNG", File(targetDir, "ic_launcher.png"))
            
            // Generate rounded icon by clipping a circle
            val roundImage = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB)
            val rg = roundImage.createGraphics()
            rg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            rg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            rg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            
            rg.clip = Ellipse2D.Double(0.0, 0.0, targetSize.toDouble(), targetSize.toDouble())
            rg.color = Color.WHITE
            rg.fillRect(0, 0, targetSize, targetSize)
            rg.drawImage(image, transform, null)
            rg.dispose()
            
            ImageIO.write(roundImage, "PNG", File(targetDir, "ic_launcher_round.png"))
            println("GENERATED ICONS FOR: $dirName")
        }
    }
}
