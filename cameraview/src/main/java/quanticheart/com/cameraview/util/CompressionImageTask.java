package quanticheart.com.cameraview.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CompressionImageTask {

    //==============================================================================================
    //
    // ** init Compresion image
    //
    //==============================================================================================

    public static Bitmap compressImage(Activity activity, byte[] data) {

        File file = makeFile(getDiretory());

        Bitmap scaledBitmap = null;
//
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);
//        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

        //max Height and width values of the compressed image is taken as 816x612

        float maxHeight = 816.0f;
        float maxWidth = 612.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

        //width and height values are set maintaining the aspect ratio of the image

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

        //setting inSampleSize value allows to load a scaled down version of the original image

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

        //inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

        //this options allow android to claim the bitmap memory if it runs low autoToOn memory
        options.inTempStorage = new byte[16 * 1024];

        try {
            //load the bitmap from its path
            bmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }
//
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        assert scaledBitmap != null;
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        try {
            FileOutputStream out = new FileOutputStream(file);
            //write the compressed bitmap at the destination specified by filename.
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ExifInterface exif = new ExifInterface(file.toString());
            String orientacao = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            int codigoOrientacao = Integer.parseInt(orientacao);
//            exif = new ExifInterface(getRealPathFromURI(activity, file.toString()));
            Log.i("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
            if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")) {
                scaledBitmap = rotate(scaledBitmap, 90);
            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")) {
                scaledBitmap = rotate(scaledBitmap, 270);
            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")) {
                scaledBitmap = rotate(scaledBitmap, 180);
            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("0")) {
                scaledBitmap = rotate(scaledBitmap, 90);
            }

//            switch (codigoOrientacao) {
//                case ExifInterface.ORIENTATION_UNDEFINED:
//                case ExifInterface.ORIENTATION_NORMAL:
//                    scaledBitmap = rotate(scaledBitmap, 90);
//                case ExifInterface.ORIENTATION_ROTATE_90:
//                    scaledBitmap = rotate(scaledBitmap, 90);
//                case ExifInterface.ORIENTATION_ROTATE_180:
//                    scaledBitmap = rotate(scaledBitmap, 180);
//                case ExifInterface.ORIENTATION_ROTATE_270:
//                    scaledBitmap = rotate(scaledBitmap, 270);
//            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return scaledBitmap;
    }

    public static Bitmap compressImage(Activity activity, File data) {

        File file = StorageUtil.INSTANCE.createFile(StorageUtil.INSTANCE.getExternalStorageDirectory(activity));

        Bitmap scaledBitmap = null;
        Bitmap original = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        original = BitmapFactory.decodeFile(data.getAbsolutePath(), options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

        //max Height and width values of the compressed image is taken as 816x612

        float maxHeight = 816.0f;
        float maxWidth = 612.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

        //width and height values are set maintaining the aspect ratio of the image
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

        //setting inSampleSize value allows to load a scaled down version of the original image
        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);
        //inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;
        //this options allow android to claim the bitmap memory if it runs low autoToOn memory
        options.inTempStorage = new byte[16 * 1024];

        try {
            //load the bitmap from its path
            original = BitmapFactory.decodeFile(data.getAbsolutePath(), options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }
//


        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        float degrees = 90;
        Matrix scaleMatrix = new Matrix();
        scaleMatrix.preRotate(degrees, original.getWidth()/2, original.getHeight()/2);
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        try {
            scaledBitmap = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), scaleMatrix, true);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        assert scaledBitmap != null;
        Canvas canvas = new Canvas(scaledBitmap);
//        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(original, scaleMatrix, new Paint(Paint.FILTER_BITMAP_FLAG));

        try {
            FileOutputStream out = new FileOutputStream(file);
            //write the compressed bitmap at the destination specified by filename.
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        try {
//            ExifInterface exif = new ExifInterface(data.toString());
//            String orientacao = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
//            int codigoOrientacao = Integer.parseInt(orientacao);
////            exif = new ExifInterface(getRealPathFromURI(activity, file.toString()));
//            Log.i("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
////            if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")) {
////                scaledBitmap = rotate(scaledBitmap, 90);
////            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")) {
////                scaledBitmap = rotate(scaledBitmap, 270);
////            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")) {
////                scaledBitmap = rotate(scaledBitmap, 180);
////            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("0")) {
////                scaledBitmap = rotate(scaledBitmap, 90);
////            }
//
//            switch (codigoOrientacao) {
//                case ExifInterface.ORIENTATION_UNDEFINED:
//                case ExifInterface.ORIENTATION_NORMAL:
//                    scaledBitmap = rotate(scaledBitmap, 90);
//                case ExifInterface.ORIENTATION_ROTATE_90:
//                    scaledBitmap = rotate(scaledBitmap, 90);
//                case ExifInterface.ORIENTATION_ROTATE_180:
//                    scaledBitmap = rotate(scaledBitmap, 180);
//                case ExifInterface.ORIENTATION_ROTATE_270:
//                    scaledBitmap = rotate(scaledBitmap, 90);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        return scaledBitmap;
    }

    //==============================================================================================
    //
    // ** Rotate Image
    //
    //==============================================================================================

    private static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        //       mtx.postRotate(degree);
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    //==============================================================================================
    //
    // ** Utils for compressions
    //
    //==============================================================================================

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    //==============================================================================================
    //
    // ** get Real Path image in Storage
    //
    //==============================================================================================
    private static String getRealPathFromURI(Activity activity, String contentURI) {
        Uri contentUri = Uri.parse(contentURI);
        Cursor cursor = activity.getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    //==============================================================================================
    //
    // ** getImage File Name
    //
    //==============================================================================================

    public static String getFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File file = new File(Environment.getExternalStorageDirectory().getPath(), "ArtEverywhere");
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/IMG_" + timeStamp + ".jpg");
        return uriSting;

    }

    //==============================================================================================
    //
    // ** init vats
    //
    //==============================================================================================

    private static final String IMAGE_DIRECTORY = "/CustomImage";

    //==============================================================================================
    //
    // ** diretoy save images
    //
    //==============================================================================================

    private static String getDiretory() {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + timeStamp + ".jpeg";
    }

    //==============================================================================================
    //
    // ** Make file
    //
    //==============================================================================================

    private static File makeFile(String diretory) {
        File pictureFile = new File(diretory);
        if (pictureFile.exists()) {
            boolean b = pictureFile.delete();
            if (b) {
                Log.w("File", "Deleted");
            }
        }

        return pictureFile;
    }
}
