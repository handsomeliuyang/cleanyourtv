package com.gathin.tvhome;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/** Four functional banners drawn locally, independent of installed apps and vendor icons. */
final class FamilyArtwork {
    private static final int[] COLORS = {0xFF8275D8, 0xFF4DAE9D, 0xFFE2AB55, 0xFFD96D74};

    static Drawable banner(Resources resources, int slot) {
        Bitmap bitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int color = COLORS[slot];
        paint.setShader(new LinearGradient(0, 0, 320, 180, color,
                Color.rgb(Color.red(color) / 2, Color.green(color) / 2, Color.blue(color) / 2), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, 320, 180, paint);
        paint.setShader(null);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3.5f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(130, 66, 190, 114, 5, 5, paint);
        if (slot == 0) {
            canvas.drawLine(130, 80, 190, 80, paint);
            for (int x = 137; x < 185; x += 16) canvas.drawLine(x, 66, x + 9, 80, paint);
        } else if (slot == 1) {
            Path hills = new Path();
            hills.moveTo(132, 104); hills.lineTo(150, 85); hills.lineTo(165, 101);
            hills.lineTo(175, 91); hills.lineTo(188, 104);
            canvas.drawPath(hills, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(175, 78, 4, paint);
        } else if (slot == 2) {
            canvas.drawLine(149, 55, 160, 66, paint);
            canvas.drawLine(171, 55, 160, 66, paint);
            canvas.drawLine(148, 121, 172, 121, paint);
        } else {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(34);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("央", 160, 102, paint);
        }
        if (slot == 0 || slot == 2) {
            Path play = new Path();
            play.moveTo(154, slot == 0 ? 86 : 78);
            play.lineTo(173, slot == 0 ? 96 : 90);
            play.lineTo(154, slot == 0 ? 107 : 102);
            play.close();
            paint.setStyle(Paint.Style.FILL);
            canvas.drawPath(play, paint);
        }
        return new BitmapDrawable(resources, bitmap);
    }

    private FamilyArtwork() {}
}
