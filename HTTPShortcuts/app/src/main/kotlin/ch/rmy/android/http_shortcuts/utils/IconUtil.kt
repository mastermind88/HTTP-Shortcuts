package ch.rmy.android.http_shortcuts.utils

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import ch.rmy.android.framework.extensions.setTintCompat
import ch.rmy.android.framework.utils.UUIDUtils.UUID_REGEX
import ch.rmy.android.framework.utils.UUIDUtils.newUUID
import ch.rmy.android.http_shortcuts.icons.ShortcutIcon
import java.io.File
import java.util.regex.Pattern
import java.util.regex.Pattern.quote
import kotlin.math.max

object IconUtil {

    private const val ICON_SCALING_FACTOR = 2

    private const val CUSTOM_ICON_NAME_PREFIX = "custom-icon_"
    private const val CUSTOM_ICON_NAME_SUFFIX = ".png"
    private const val CUSTOM_ICON_NAME_ALTERNATIVE_SUFFIX = ".jpg"

    private val CUSTOM_ICON_NAME_REGEX = "${quote(CUSTOM_ICON_NAME_PREFIX)}($UUID_REGEX)" +
        "(${quote(CUSTOM_ICON_NAME_SUFFIX)}|${quote(CUSTOM_ICON_NAME_ALTERNATIVE_SUFFIX)})"
    private val CUSTOM_ICON_NAME_PATTERN = CUSTOM_ICON_NAME_REGEX.toPattern(Pattern.CASE_INSENSITIVE)

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun getIcon(context: Context, icon: ShortcutIcon): Icon? = try {
        when (icon) {
            is ShortcutIcon.NoIcon -> {
                Icon.createWithResource(context.packageName, ShortcutIcon.NoIcon.ICON_RESOURCE)
            }
            is ShortcutIcon.ExternalResourceIcon -> {
                Icon.createWithResource(icon.packageName, icon.resourceId)
            }
            is ShortcutIcon.CustomIcon -> {
                val file = icon.getFile(context)
                if (file == null) {
                    Icon.createWithResource(context.packageName, ShortcutIcon.NoIcon.ICON_RESOURCE)
                } else {
                    val options = BitmapFactory.Options()
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    Icon.createWithBitmap(bitmap)
                }
            }
            is ShortcutIcon.BuiltInIcon -> {
                val file = generateRasterizedIconForBuiltInIcon(context, icon)
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                Icon.createWithBitmap(bitmap)
            }
        }
    } catch (e: Exception) {
        null
    }

    fun generateRasterizedIconForBuiltInIcon(
        context: Context,
        icon: ShortcutIcon.BuiltInIcon,
    ): File {
        val fileName = "icon_${icon.iconName}.png"
        val file = context.getFileStreamPath(fileName)
        if (file.exists()) {
            return file
        }

        val identifier = icon.getDrawableIdentifier(context)
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmap = getBitmapFromVectorDrawable(context, identifier, icon.tint)
        context.openFileOutput(fileName, 0).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.flush()
        }
        bitmap.recycle()
        return file
    }

    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int, tint: Int?): Bitmap {
        val drawable = AppCompatResources.getDrawable(context, drawableId)!!
        val iconSize = getIconSize(context)
        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, iconSize, iconSize)
        if (tint != null) {
            drawable.setTintCompat(tint)
        }
        drawable.draw(canvas)
        return bitmap
    }

    fun getIconSize(context: Context, scaled: Boolean = true): Int {
        if (iconSizeCached == null) {
            iconSizeCached = max(
                context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size),
                (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).launcherLargeIconSize
            )
        }
        return if (scaled) {
            iconSizeCached!! * ICON_SCALING_FACTOR
        } else {
            iconSizeCached!!
        }
    }

    private var iconSizeCached: Int? = null

    fun isCustomIconName(string: String) =
        string.matches(CUSTOM_ICON_NAME_REGEX.toRegex())

    fun generateCustomIconName(): String =
        "$CUSTOM_ICON_NAME_PREFIX${newUUID()}$CUSTOM_ICON_NAME_SUFFIX"

    fun extractCustomIconNames(string: String): Set<String> {
        val result = mutableSetOf<String>()
        val matcher = CUSTOM_ICON_NAME_PATTERN.matcher(string)
        while (matcher.find()) {
            result.add(matcher.group())
        }
        return result
    }

    fun getCustomIconNamesInApp(context: Context): List<String> =
        context.filesDir
            .listFiles { file ->
                file.name.matches(CUSTOM_ICON_NAME_REGEX.toRegex())
            }
            ?.map { it.name }
            ?: emptyList()
}
